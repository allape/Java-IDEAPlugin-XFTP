package net.allape.xftp

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.remote.RemoteConnectionType
import com.intellij.remote.RemoteCredentials
import com.intellij.ssh.ConnectionBuilder
import com.intellij.ssh.RemoteFileObject
import com.intellij.ssh.SshTransportException
import com.intellij.ssh.channels.SftpChannel
import com.intellij.ssh.connectionBuilder
import com.intellij.ui.JBSplitter
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.util.ReflectionUtil
import com.jetbrains.plugins.remotesdk.console.SshConfigConnector
import com.jetbrains.plugins.remotesdk.console.SshTerminalDirectRunner
import icons.TerminalIcons
import net.allape.action.ActionToolbarFastEnableAnAction
import net.allape.common.HistoryTopicHandler
import net.allape.common.RemoteDataProducerWrapper
import net.allape.common.Services
import net.allape.common.Windows
import net.allape.component.FileTable
import net.allape.component.FileTableModel
import net.allape.component.MemoComboBox
import net.allape.model.*
import net.allape.util.Maps
import net.schmizz.sshj.common.StreamCopier
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.sftp.SFTPFileTransfer
import net.schmizz.sshj.xfer.TransferListener
import org.jetbrains.plugins.terminal.TerminalTabState
import org.jetbrains.plugins.terminal.TerminalView
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.net.SocketException
import java.nio.charset.Charset
import java.util.*
import java.util.stream.Collectors
import javax.swing.DropMode
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import kotlin.math.roundToInt

open class SuperExplorer(
    protected val project: Project,
    protected val toolWindow: ToolWindow,
    protected val application: Application,
) : Disposable {

    companion object {
        // 服务器文件系统分隔符
        const val SERVER_FILE_SYSTEM_SEPARATOR = "/"

        // 默认集合大小
        const val COLLECTION_SIZE = 100

        // 当前用户本地home目录
        val USER_HOME: String = System.getProperty("user.home")

        // 双击间隔, 毫秒
        const val DOUBLE_CLICK_INTERVAL: Long = 350

        // 最大可打开文件
        const val EDITABLE_FILE_SIZE = (2 * 1024 * 1024).toLong()

        // 双击监听
        var clickWatcher = System.currentTimeMillis()

        // 远程文件拖拽flavor
        val remoteFileListFlavor = DataFlavor(RemoteFileObject::class.java, "SSH remote file list")

        // region UI配置

        const val LOCAL_HISTORY_PERSISTENCE_KEY = "xftp.persistence.local-history"
        const val REMOTE_HISTORY_PERSISTENCE_KEY = "xftp.persistence.remote-history"

        const val LOCAL_TOOL_BAR_PLACE = "XFTPLocalToolBar"
        const val REMOTE_TOOL_BAR_PLACE = "XFTPRemoteToolBar"

        private fun defaultConfig(): GridBagConstraints {
            val gridBagCons = GridBagConstraints()
            gridBagCons.fill = GridBagConstraints.BOTH
            gridBagCons.weightx = 1.0
            gridBagCons.weighty = 1.0
            return gridBagCons
        }

        val X0Y0: GridBagConstraints = defaultConfig()

        init {
            X0Y0.gridx = 0
            X0Y0.gridy = 0
        }

        val X0Y1: GridBagConstraints = defaultConfig()

        init {
            X0Y1.gridx = 0
            X0Y1.gridy = 1
        }

        val X1Y0: GridBagConstraints = defaultConfig()

        init {
            X1Y0.gridx = 1
            X1Y0.gridy = 0
        }

        // endregion
    }

    override fun dispose() {}

}

open class ExplorerUI(
    project: Project,
    toolWindow: ToolWindow,
) : SuperExplorer(project, toolWindow, ApplicationManager.getApplication()) {

    protected var panelWrapper = JPanel(BorderLayout())
    protected var splitter: JBSplitter = OnePixelSplitter("xftp-main-window", .5f)

    protected var localWrapper = JPanel(GridBagLayout())
    protected var localPathWrapper = JPanel(GridBagLayout())
    protected var localPath = MemoComboBox<String>(LOCAL_HISTORY_PERSISTENCE_KEY)
    protected var localFileList: FileTable = FileTable()
    protected var localFileListWrapper: JBScrollPane = JBScrollPane(localFileList)
    protected var localActionGroup = DefaultActionGroup()
    protected var localActionToolBar = ActionToolbarImpl(LOCAL_TOOL_BAR_PLACE, localActionGroup, false)
    protected var remoteWrapper = JPanel(GridBagLayout())

    protected var remotePathWrapper = JPanel(GridBagLayout())
    protected var remotePath = MemoComboBox<String>(REMOTE_HISTORY_PERSISTENCE_KEY)
    protected var remoteActionGroup = DefaultActionGroup()
    protected var remoteActionToolBar = ActionToolbarImpl(REMOTE_TOOL_BAR_PLACE, remoteActionGroup, false)
    protected var remoteFileList = FileTable()
    protected var remoteFileListWrapper = JBScrollPane(remoteFileList)

    /**
     * 初始化UI样式
     */
    protected open fun initUI() {
        val noYWeightX0Y0 = X0Y0.clone() as GridBagConstraints
        noYWeightX0Y0.weighty = 0.0
        val noXWeightX0Y0 = X0Y0.clone() as GridBagConstraints
        noXWeightX0Y0.weightx = 0.0
        val noXWeightX1Y0 = X1Y0.clone() as GridBagConstraints
        noXWeightX1Y0.weightx = 0.0

        // region 本地
        localPath.setMinimumAndPreferredWidth(100)
        localPathWrapper.add(localPath, noYWeightX0Y0)
        localFileListWrapper.border = null
        localPathWrapper.add(localFileListWrapper, X0Y1)
        localWrapper.add(localPathWrapper, X0Y0)
        val localActionToolBarWrapper = JPanel(BorderLayout())
        localActionToolBarWrapper.minimumSize = Dimension(48, 0)
        localActionToolBarWrapper.add(localActionToolBar)
        localWrapper.add(localActionToolBarWrapper, noXWeightX1Y0)
        // endregion

        // region 远程
        val remoteActionToolBarWrapper = JPanel(BorderLayout())
        remoteActionToolBarWrapper.minimumSize = Dimension(48, 0)
        remoteActionToolBarWrapper.add(remoteActionToolBar)
        remoteWrapper.add(remoteActionToolBarWrapper, noXWeightX0Y0)
        remotePath.setMinimumAndPreferredWidth(100)
        remotePathWrapper.add(remotePath, noYWeightX0Y0)
        remoteFileListWrapper.border = null
        remotePathWrapper.add(remoteFileListWrapper, X0Y1)
        remoteWrapper.add(remotePathWrapper, X1Y0)
        // endregion
        localWrapper.border = null
        remoteWrapper.border = null
        splitter.firstComponent = localWrapper
        splitter.secondComponent = remoteWrapper
        panelWrapper.add(splitter, BorderLayout.CENTER)
        panelWrapper.border = null
    }

    /**
     * 获取当前window的UI内容
     */
    open fun getUI(): JComponent {
        return panelWrapper
    }

    /**
     * 设置当前远程内容锁定
     */
    protected open fun lockRemoteUIs() {
        application.invokeLater { remoteWrapper.isEnabled = false }
    }

    /**
     * 设置当前远程内容锁定
     */
    protected open fun unlockRemoteUIs() {
        application.invokeLater { remoteWrapper.isEnabled = true }
    }

}

class Explorer(
    project: Project,
    toolWindow: ToolWindow,
) : ExplorerUI(project, toolWindow) {

    // 传输历史记录topic的publisher
    private val historyTopicHandler: HistoryTopicHandler = project.messageBus.syncPublisher(HistoryTopicHandler.HISTORY_TOPIC)

    // 传输历史
    private val history: ArrayList<Transfer> = ArrayList(100)

    // 传输中的
    private val transferring: ArrayList<Transfer> = ArrayList(100)

    // 窗口content
    private lateinit var content: Content

    // 上一次选择文件
    private var lastFile: FileModel? = null

    // 当前选中的本地文件
    private var currentLocalFile: FileModel? = null

    // 当前选中的远程文件
    private var currentRemoteFile: FileModel? = null

    // 修改中的远程文件, 用于文件修改后自动上传 key: remote file, value: local file
    private val remoteEditingFiles: HashMap<RemoteFileObject, String> = HashMap(COLLECTION_SIZE)

    // 当前配置的连接器
    private var connector: SshConfigConnector? = null
    // 当前配置
    private var credentials: RemoteCredentials? = null
    // 当前配置的连接创建者
    private var connectionBuilder: ConnectionBuilder? = null
    // 当前开启的channel
    private var sftpChannel: SftpChannel? = null
    // 当前channel中的sftp client
    private var sftpClient: SFTPClient? = null

    // region actions图标按钮

    // 建立连接
    private val explore: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
        remoteActionToolBar,
        "Start New Session", "Start a sftp session",
        AllIcons.Webreferences.Server
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            connectSftp()
        }
    }

    // 显示combobox的下拉内容
    private val dropdown: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
        remoteActionToolBar,
        "Dropdown", "Display remote access history",
        AllIcons.Actions.MoveDown
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            remotePath.isPopupVisible = true
        }
    }

    // 刷新
    private val reload: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
        remoteActionToolBar,
        "Reload Remote", "Reload current remote folder",
        AllIcons.Actions.Refresh
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            reloadRemote()
        }
    }

    // 断开连接
    private val suspend: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
        remoteActionToolBar,
        "Disconnect", "Disconnect from sftp server",
        AllIcons.Actions.Suspend
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            if (MessageDialogBuilder.yesNo("Disconnecting", "Do you really want to close this session?")
                    .asWarning()
                    .yesText("Disconnect")
                    .ask(project)
            ) {
                disconnect(true)
            }
        }
    }

    // 命令行打开
    private val newTerminal: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
        remoteActionToolBar,
        "Open In Terminal", "Open current folder in ssh terminal",
        TerminalIcons.OpenTerminal_13x13
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            val state = TerminalTabState()
            state.myWorkingDirectory = remotePath.getMemoItem()
            TerminalView.getInstance(project).createNewSession(
                SshTerminalDirectRunner(project, credentials, Charset.defaultCharset()),
                state
            )
        }
    }

    // 隐藏本地浏览器
    private val localToggle: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
        remoteActionToolBar,
        "Toggle Local Explorer", "Hide or display local file list",
        AllIcons.Diff.ApplyNotConflictsRight
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            val to: Boolean = !splitter.firstComponent.isVisible
            splitter.firstComponent.isVisible = to
            this.setIcon(if (to) AllIcons.Diff.ApplyNotConflictsRight else AllIcons.Diff.ApplyNotConflictsLeft)
        }
    }

    // endregion

    init {
        setCurrentLocalPath(this.project.basePath ?: "/")

        // 初始化文件监听
        this.project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                for (e in events) {
                    if (e.isFromSave) {
                        val virtualFile = e.file
                        if (virtualFile != null) {
                            val localFile = virtualFile.path
                            Maps.getFirstKeyByValue(remoteEditingFiles, localFile)?.let { remoteFile ->
                                // 上传文件
                                application.executeOnPooledThread {
                                    transfer(File(localFile), remoteFile, TransferType.UPLOAD)
                                }
                            }
                        }
                    }
                }
            }
        })

        initUI()
    }

    override fun dispose() {
        super.dispose()

        // 关闭连接
        disconnect(true)
    }

    override fun initUI() {
        super.initUI()

        localActionGroup.addAll(
            object : DumbAwareAction(
                "Refresh Local",
                "Refresh current local folder",
                AllIcons.Actions.Refresh
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    reloadLocal()
                }
            },
            object : DumbAwareAction(
                "Open In FileManager",
                "Display folder in system file manager",
                AllIcons.Actions.MenuOpen
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    localPath.getMemoItem()?.let { path ->
                        try {
                            Desktop.getDesktop().open(File(path))
                        } catch (ioException: IOException) {
                            ioException.printStackTrace()
                            Services.message("Failed to open \"$path\"", MessageType.ERROR)
                        }
                    }
                }
            }
        )

        localPath.addActionListener {
            var path = localPath.getMemoItem()
            if (path == null || path.isEmpty()) {
                path = "/"
            }
            try {
                val file = File(path)
                if (!file.exists()) {
                    Services.message("$path does not exist!", MessageType.INFO)
                    // 获取第二个历史, 不存在时加载项目路径
                    localPath.getItemAt(1)?.let { lastPath ->
                        if (lastPath.memo != null) {
                            setCurrentLocalPath(lastPath.memo.toString())
                        } else {
                            setCurrentLocalPath(Objects.requireNonNull(project.basePath)!!)
                        }
                    }
                } else if (!file.isDirectory) {
                    // 加载父文件夹
                    setCurrentLocalPath(file.parent)
                    if (file.length() > EDITABLE_FILE_SIZE) {
                        if (MessageDialogBuilder.yesNo(
                                "This file is too large for text editor",
                                "Do you still want to edit it?"
                            )
                                .asWarning()
                                .yesText("Edit it")
                                .ask(project)
                        ) {
                            openFileInEditor(file)
                        }
                    } else {
                        openFileInEditor(file)
                    }
                } else {
                    val files = file.listFiles()
                    if (files == null) {
                        if (MessageDialogBuilder.yesNo(
                                "It is an unavailable folder!",
                                "This folder is not available, do you want to open it in system file manager?"
                            )
                                .asWarning()
                                .yesText("Open")
                                .ask(project)
                        ) {
                            try {
                                Desktop.getDesktop().open(file)
                            } catch (ioException: IOException) {
                                ioException.printStackTrace()
                                Services.message(
                                    "Failed to open file in system file manager",
                                    MessageType.INFO
                                )
                            }
                        }
                        return@addActionListener
                    }
                    path = file.absolutePath
                    val fileModels: MutableList<FileModel> = ArrayList(
                        if (file.length() == 0L) 1 else if (file.length() > Int.MAX_VALUE) Int.MAX_VALUE else file.length()
                            .toString().toInt()
                    )

                    // 添加返回上一级目录
                    fileModels.add(getParentFolder(path, File.separator))
                    for (currentFile in files) {
                        fileModels.add(
                            FileModel(
                                currentFile.absolutePath,
                                currentFile.name,
                                currentFile.isDirectory,
                                currentFile.length(),
                                (if (currentFile.canRead()) 4 else 0) or
                                        (if (currentFile.canWrite()) 2 else 0) or
                                        if (currentFile.canExecute()) 1 else 0,
                                true
                            )
                        )
                    }
                    localFileList.resetData(fileModels)
                }
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                Services.message(ex.message!!, MessageType.WARNING)
            }
        }

        localFileList.selectionModel.addListSelectionListener {
            localFileList.model.data.let { allRemoteFiles ->
                localFileList.selectedRow.let { currentSelectRow ->
                    if (currentSelectRow != -1) {
                        lastFile = currentLocalFile
                        currentLocalFile = allRemoteFiles[currentSelectRow]
                    }
                }
            }
        }
        // 监听双击, 双击后打开文件或文件夹
        localFileList.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1) {
                    val now = System.currentTimeMillis()
                    if (lastFile != null && lastFile === currentLocalFile && now - clickWatcher < DOUBLE_CLICK_INTERVAL) {
                        setCurrentLocalPath(currentLocalFile!!.path)
                    }
                    clickWatcher = now
                }
            }
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseEntered(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {}
        })
        localFileList.dragEnabled = true
        localFileList.transferHandler = object : FileTransferHandler<File>() {
            override fun createTransferable(c: JComponent?): Transferable {
                val fileModels: List<FileModel> = getSelectedFileList(localFileList)
                val files: MutableList<File> = ArrayList(fileModels.size)
                for (path in fileModels) {
                    files.add(File(path.path))
                }
                return FileTransferable(files, DataFlavor.javaFileListFlavor)
            }

            override fun canImport(support: TransferSupport): Boolean {
                return if (!support.isDrop) {
                    false
                } else support.isDataFlavorSupported(remoteFileListFlavor)
            }

            override fun importData(support: TransferSupport): Boolean {
                try {
                    localPath.getMemoItem()?.let { currentLocalPath ->
                        @Suppress("UNCHECKED_CAST")
                        for (file in support.transferable.getTransferData(remoteFileListFlavor) as List<RemoteFileObject>) {
                            application.executeOnPooledThread {
                                transfer(
                                    File(currentLocalPath + File.separator + file.name()),
                                    file,
                                    TransferType.DOWNLOAD,
                                )
                            }
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                return false
            }
        }

        remotePath.isEnabled = false
        remotePath.addActionListener {
            val remoteFilePath = remotePath.getMemoItem() ?: return@addActionListener
            application.executeOnPooledThread {
                if (sftpChannel == null) {
                    Services.message("Please connect to server first!", MessageType.INFO)
                } else if (!sftpChannel!!.isConnected) {
                    Services.message("SFTP lost connection, retrying...", MessageType.ERROR)
                    connectSftp()
                }
                remoteFileList.isEnabled = false
                remotePath.isEnabled = false
                // 是否接下来加载父文件夹
                var loadParent: String? = null
                try {
                    var path =
                        remoteFilePath.ifEmpty { SERVER_FILE_SYSTEM_SEPARATOR }
                    val file = sftpChannel!!.file(path)
                    path = file.path()
                    if (!file.exists()) {
                        Services.message("$path does not exist!", MessageType.INFO)
                        remotePath.getItemAt(1)?.let { lastPath ->
                            if (lastPath.memo != null) {
                                setCurrentRemotePath(lastPath.memo.toString())
                            } else {
                                setCurrentRemotePath(sftpChannel!!.home)
                            }
                        }
                    } else if (!file.isDir()) {
                        downloadFileAndEdit(file)
                        loadParent = getParentFolderPath(file.path(), SERVER_FILE_SYSTEM_SEPARATOR)
                    } else {
                        val files = file.list()
                        val fileModels: MutableList<FileModel> =
                            ArrayList(files.size)

                        // 添加返回上一级目录
                        fileModels.add(getParentFolder(path, SERVER_FILE_SYSTEM_SEPARATOR))
                        for (f in files) {
                            fileModels.add(
                                FileModel(
                                    // 处理有些文件夹是//开头的
                                    normalizeRemoteFileObjectPath(f),
                                    f.name(), f.isDir(), f.size(), f.permissions, false
                                )
                            )
                        }
                        application.invokeLater { remoteFileList.resetData(fileModels) }
                    }
                } catch (ex: java.lang.Exception) {
                    ex.printStackTrace()
                    Services.message(
                        "Error occurred while listing remote files: " + ex.message,
                        MessageType.ERROR
                    )
                } finally {
                    remoteFileList.isEnabled = true
                    remotePath.isEnabled = true
                    if (loadParent != null) {
                        // 加载父文件夹
                        setCurrentRemotePath(loadParent)
                    }
                }
            }
        }


        remoteActionGroup.addAll(
            explore,
            dropdown,
            reload,
            suspend,
            newTerminal,
            Separator.create(),
            localToggle
        )
        setRemoteButtonsEnable(false)

        remoteFileList.selectionModel.addListSelectionListener {
            val allRemoteFiles: List<FileModel> = remoteFileList.model.data
            val currentSelectRow = remoteFileList.selectedRow
            if (currentSelectRow != -1) {
                lastFile = currentRemoteFile
                currentRemoteFile = allRemoteFiles[currentSelectRow]
            }
        }
        remoteFileList.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1) {
                    val now = System.currentTimeMillis()
                    if (lastFile != null && lastFile === currentRemoteFile && now - clickWatcher < DOUBLE_CLICK_INTERVAL) {
                        setCurrentRemotePath(currentRemoteFile!!.path)
                    }
                    clickWatcher = now
                }
            }
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseEntered(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {}
        })
        remoteFileList.dragEnabled = true
        remoteFileList.dropMode = DropMode.ON
        remoteFileList.transferHandler = object : FileTransferHandler<RemoteFileObject?>() {
            override fun createTransferable(c: JComponent?): Transferable {
                return FileTransferable<RemoteFileObject>(getSelectedRemoteFileList(), remoteFileListFlavor)
            }

            override fun canImport(support: TransferSupport): Boolean {
                if (sftpChannel == null || !sftpChannel!!.isConnected) {
                    return false
                } else if (!support.isDrop) {
                    return false
                }
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
            }

            override fun importData(support: TransferSupport): Boolean {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val files = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                    remotePath.getMemoItem()?.let { currentRemotePath ->
                        for (file in files) {
                            application.executeOnPooledThread {
                                transfer(
                                    file,
                                    sftpChannel!!.file(currentRemotePath + SERVER_FILE_SYSTEM_SEPARATOR + file.name),
                                    TransferType.UPLOAD
                                )
                            }
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                return false
            }
        }


        val remoteFileListPopupMenuDelete = JBMenuItem("rm -Rf")
        remoteFileListPopupMenuDelete.addActionListener {
            if (!MessageDialogBuilder.yesNo("Delete Confirm", "This operation is irreversible, continue?")
                    .asWarning()
                    .yesText("Cancel")
                    .noText("Continue")
                    .ask(project)
            ) {
                val files: List<RemoteFileObject> = getSelectedRemoteFileList()
                application.invokeLater {
                    lockRemoteUIs()
                    application.executeOnPooledThread {
                        // 开启一个ExecChannel来删除非空的文件夹
                        try {
                            for (file in files) {
                                try {
                                    connectionBuilder!!.execBuilder("rm -Rf " + file.path()).execute().waitFor()
                                    // file.rm();
                                } catch (err: java.lang.Exception) {
                                    err.printStackTrace()
                                    Services.message(
                                        "Error occurred while delete file: " + file.path(),
                                        MessageType.ERROR
                                    )
                                }
                            }
                        } catch (err: java.lang.Exception) {
                            err.printStackTrace()
                            Services.message(
                                "Can't delete file or folder right now, please try it later",
                                MessageType.ERROR
                            )
                        } finally {
                            application.invokeLater {
                                unlockRemoteUIs()
                                // 刷新当前页面
                                reloadRemote()
                            }
                        }
                    }
                }
            }
        }
        
        val remoteFileListPopupMenu = JBPopupMenu()
        remoteFileListPopupMenu.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                remoteFileListPopupMenuDelete.isEnabled = getSelectedRemoteFileList().isNotEmpty()
            }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {}
            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        })
        remoteFileListPopupMenu.add(remoteFileListPopupMenuDelete)
        remoteFileList.componentPopupMenu = remoteFileListPopupMenu
    }

    /**
     * 设置需要进行连接后才能使用的按钮的状态
     * @param enable 是否启用
     */
    private fun setRemoteButtonsEnable(enable: Boolean) {
        explore.setEnabled(!enable)
        dropdown.setEnabled(enable)
        reload.setEnabled(enable)
        suspend.setEnabled(enable)
        newTerminal.setEnabled(enable)
    }

    /**
     * 使用当前选择的配置进行连接
     */
    fun connectSftp() {
        try {
            RemoteDataProducerWrapper()
                .withProject(project)
                .produceRemoteDataWithConnector(
                    null,
                    null
                ) { c ->
                    c?.let { connector ->
                        this@Explorer.connector = connector as SshConfigConnector
                        connector.produceRemoteCredentials { data ->
                            if (data != null) {
                                credentials = data
                                this.triggerConnecting()
                                this.disconnect(false)

                                this.application.executeOnPooledThread {
                                    // com.jetbrains.plugins.remotesdk.tools.RemoteTool.startRemoteProcess
                                    @Suppress("UnstableApiUsage")
                                    connectionBuilder = credentials!!.connectionBuilder(
                                        project,
                                        ProgressManager.getGlobalProgressIndicator(),
                                        true
                                    ).withConnectionTimeout(30L)

                                    ProgressManager.getInstance().run(object : Task.Backgroundable(
                                        project,
                                        "Connecting to " + getWindowName(),
                                        // 暂时不允许取消
                                        false
                                    ) {
                                        // 是否已被取消
                                        private var cancelled = false

                                        override fun run(indicator: ProgressIndicator) {
                                            indicator.isIndeterminate = true
                                            try {
                                                sftpChannel = connectionBuilder!!.openSftpChannel()
                                                triggerConnected()

                                                try {
                                                    // 获取sftpClient
                                                    // 使用反射获取到 net.schmizz.sshj.sftp.SFTPClient
                                                    @Suppress("INVISIBLE_REFERENCE")
                                                    val sftpChannelClass = com.intellij.ssh.impl.sshj.channels.SshjSftpChannel::class.java
                                                    val field = ReflectionUtil.findFieldInHierarchy(sftpChannelClass) { f: Field ->
                                                        f.type == SFTPClient::class.java
                                                    } ?: throw IllegalArgumentException("Unable to upload files!")
                                                    field.isAccessible = true
                                                    sftpClient = field[sftpChannel] as SFTPClient
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Services.message(
                                                        "Failed to get sftp client for this session, please try it again: ${e.message}",
                                                        MessageType.ERROR
                                                    )
                                                }

                                                application.invokeLater { setCurrentRemotePath(sftpChannel!!.home) }
                                            } catch (e: SshTransportException) {
                                                if (!cancelled) {
                                                    Services.message("Failed to connect: " + e.message, MessageType.WARNING)
                                                }
                                                triggerDisconnected()
                                                e.printStackTrace()
                                            }
                                        }

                                        override fun onCancel() {
                                            super.onCancel()
                                            cancelled = true
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            this.triggerDisconnected()
            Services.message(
                e.message ?: "Failed to create a connection, please try it later",
                MessageType.ERROR,
            )
        }
    }

    /**
     * 断开当前连接
     */
    fun disconnect(triggerEvent: Boolean) {
        // 断开连接
        if (sftpChannel != null && sftpChannel!!.isConnected) {
            try {
                sftpChannel!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        connectionBuilder = null
        sftpChannel = null
        sftpClient = null
        content.displayName = Windows.WINDOW_DEFAULT_NAME
        if (triggerEvent) {
            triggerDisconnected()
        }
    }

    /**
     * 设置当前状态为连接中
     */
    private fun triggerConnecting() {
        application.invokeLater { explore.setEnabled(false) }
    }

    /**
     * 设置当前状态为已连接
     */
    private fun triggerConnected() {
        application.invokeLater {
            remotePath.isEnabled = true
            this.setRemoteButtonsEnable(true)
            if (credentials != null) {
                content.displayName = getWindowName()
//                credentials!!.userName +
//                        "@" +
//                        credentials!!.host
            }
        }
    }

    /**
     * 设置当前状态为未连接
     */
    private fun triggerDisconnected() {
        application.invokeLater {

            // 设置远程路径输入框
            remotePath.setItem(null)
            remotePath.isEnabled = false
            this.setRemoteButtonsEnable(false)

            // 清空列表
            remoteFileList.model.resetData(emptyList())
            // 清空文件列表
            remoteEditingFiles.clear()
        }
    }

    /**
     * 设置当前显示的本地路径
     * @param currentLocalPath 本地文件路径
     */
    fun setCurrentLocalPath(currentLocalPath: String) {
        val fireEventManually = currentLocalPath == localPath.getMemoItem()
        localPath.push(currentLocalPath)
        if (fireEventManually) {
            localPath.actionPerformed(null)
        }
    }

    /**
     * 刷新本地资源
     */
    fun reloadLocal() {
        setCurrentLocalPath(localPath.getMemoItem()!!)
    }

    /**
     * 谁当前显示的远程路径
     * @param currentRemotePath 远程文件路径
     */
    fun setCurrentRemotePath(currentRemotePath: String) {
        val fireEventManually = currentRemotePath == remotePath.getMemoItem()
        remotePath.push(currentRemotePath)
        if (fireEventManually) {
            remotePath.actionPerformed(null)
        }
    }

    /**
     * 刷新远程资源
     */
    fun reloadRemote() {
        setCurrentRemotePath(remotePath.getMemoItem()!!)
    }

    /**
     * 获取上一级目录
     * @param path 当前目录
     * @param separator 文件系统分隔符
     */
    private fun getParentFolder(path: String, separator: String): FileModel {
        return FileModel(getParentFolderPath(path, separator), "..", true, 0, 0, false)
    }

    /**
     * 获取上一级目录
     * @param path 当前目录
     * @param separator 文件系统分隔符
     */
    private fun getParentFolderPath(path: String, separator: String): String {
        // 添加返回上一级目录
        val lastIndexOfSep = path.lastIndexOf(separator)
        val parentFolder = if (lastIndexOfSep == -1 || lastIndexOfSep == 0) "" else path.substring(0, lastIndexOfSep)
        return if (parentFolder.isEmpty()) separator else parentFolder
    }

    /**
     * 格式化远程文件的路径, 因为有的文件开头是//
     * @param file 远程文件信息
     * @return 格式化后的文件路径
     */
    private fun normalizeRemoteFileObjectPath(file: RemoteFileObject): String {
        return if (file.path().startsWith("//")) file.path().substring(1) else file.path()
    }

    /**
     * 获取当前选中了的远程文件列表
     * @return 远程文件列表
     */
    private fun getSelectedRemoteFileList(): List<RemoteFileObject> {
        return this.getSelectedFileList(remoteFileList)
            .stream()
            .map{ (path): FileModel ->
                sftpChannel!!.file(
                    path
                )
            }
            .collect(Collectors.toList())
    }

    /**
     * 获取当前选中了的远程文件列表
     * @param fileTable 要获取选中文件的文件列表
     * @return 远程文件列表
     */
    private fun getSelectedFileList(fileTable: FileTable): List<FileModel> {
        val rows = fileTable.selectedRows
        val files: MutableList<FileModel> = ArrayList(rows.size)
        if (rows.isEmpty()) {
            return files
        }
        val fileModels = fileTable.model.data
        for (row in rows) {
            if (row == 0) continue
            files.add(fileModels[row])
        }
        return files
    }

    /**
     * 获取当前连接名称
     */
    private fun getWindowName(): String {
        return if (connector != null) connector!!.name
            else if (credentials != null) "${credentials!!.userName}@${credentials!!.host}:${credentials!!.port}"
            else Windows.WINDOW_DEFAULT_NAME
    }

    /**
     * 下载文件并编辑
     * @param remoteFile 远程文件信息
     */
    private fun downloadFileAndEdit(remoteFile: RemoteFileObject) {
        require(!remoteFile.isDir()) { "Can not edit a folder!" }
        application.invokeLater {
            // 如果文件小于2M, 则自动下载到缓存目录并进行监听
            if (remoteFile.size() > EDITABLE_FILE_SIZE) {
                if (
                    !MessageDialogBuilder.yesNo(
                        "This file is too large for text editor",
                        "Do you still want to download and edit it?"
                    ).asWarning()
                        .yesText("Do it")
                        .ask(project)
                ) {
                    return@invokeLater
                }
            }

            // 如果当前远程文件已经在编辑器中打开了, 则关闭之前的
            remoteEditingFiles
                .keys.stream()
                .filter { rf: RemoteFileObject -> rf.path() == remoteFile.path() }
                .findFirst()
                .orElse(null)?.let { existsRemoteFile ->
                    remoteEditingFiles[existsRemoteFile]?.let { localFile ->
                        val oldCachedFile = File(localFile)
                        if (oldCachedFile.exists()) {
                            if (MessageDialogBuilder.yesNo(
                                    "This file is editing",
                                    "Do you want to replace current editing file?\n\n" +
                                            "\"Open\" to reopen/focus the existing file.\n" +
                                            "\"Replace\" to re-download the file from remote and open it."
                                )
                                    .asWarning()
                                    .noText("Replace")
                                    .yesText("Open")
                                    .ask(project)
                            ) {
                                openFileInEditor(oldCachedFile)
                                return@invokeLater
                            } else {
                                remoteEditingFiles.remove(existsRemoteFile)
                            }
                        }
                    }
                }
            try {
                val formattedFileName = remoteFile.name()
                // 所有点"."开头的文件名可能导致本地fs错误
//                if (remoteFile.name().startsWith(".")) {
//                    formattedFileName = remoteFile.name().replaceAll("\\.", "-");
//                } else {
//                    String fileSuffix = remoteFile.name().contains(".") ?
//                            remoteFile.name().substring(remoteFile.name().lastIndexOf('.')) :
//                            "";
//                    formattedFileName = "-" + remoteFile.name()
//                            .substring(0, remoteFile.name().length() - fileSuffix.length())
//                            .replaceAll("\\.", "-") + fileSuffix;
//                }
                val localFile = File.createTempFile("jb-ide-xftp-", formattedFileName)
                application.executeOnPooledThread {
                    transfer(localFile, remoteFile, TransferType.DOWNLOAD, object : OnTransferResult {
                        override fun onResult(result: Transfer) {
                            if (result.result == TransferResult.SUCCESS) {
                                openFileInEditor(localFile)
                                // 加入文件监听队列
                                remoteEditingFiles[remoteFile] = localFile.absolutePath
                            }
                        }
                    })
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Services.message("Unable to create cache file: " + e.message, MessageType.ERROR)
            }
        }
    }

    /**
     * 打开文件<br/>
     * 参考: [com.intellij.openapi.fileEditor.impl.text.FileDropHandler.openFiles]
     * @param file 文件
     */
    private fun openFileInEditor(file: File) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (virtualFile == null) {
            Services.message("${file.absolutePath} does not exists!")
            return
        }
        NonProjectFileWritingAccessProvider.allowWriting(listOf(virtualFile))
        application.invokeLater {
            FileEditorManager.getInstance(project).openTextEditor(
                OpenFileDescriptor(
                    project,
                    virtualFile,
                    0
                ),
                true
            )
        }
    }

    // region 上传下载

    @Synchronized
    private fun transfer(
        localFile: File,
        remoteFile: RemoteFileObject,
        type: TransferType,
        onTransferResult: OnTransferResult? = null,
    ) {
        if (sftpChannel == null || !sftpChannel!!.isConnected) {
            Services.message("Please start a sftp session first!", MessageType.INFO)
            throw IllegalStateException("No SFTP session available!")
        }

        // 格式化远程路径
        val normalizedRemotePath = getWindowName() + ":" + normalizeRemoteFileObjectPath(remoteFile)
        // 远程路径
        val remoteFilePath = remoteFile.path()
        // 本地绝对路径
        val localFileAbsPath = localFile.absolutePath

        // 检查当前传输的队列, 存在相同target的, 取消上一个任务
        for (exists in transferring) {
            // 移除非运行中的内容
            if (exists.result !== TransferResult.TRANSFERRING) {
                application.invokeLater { transferring.remove(exists) }
            } else if (type === exists.type && exists.target == if (type === TransferType.UPLOAD) normalizedRemotePath else localFileAbsPath) {
                exists.result = TransferResult.CANCELLED
            }
        }

        // 设置传输对象
        val transfer = Transfer(
            type = type,
            source = if (type === TransferType.UPLOAD) localFileAbsPath else normalizedRemotePath,
            target = if (type === TransferType.UPLOAD) normalizedRemotePath else localFileAbsPath,
            size = if (type === TransferType.UPLOAD) localFile.length() else remoteFile.size(),
        )

        transferring.add(transfer)
        history.add(transfer)
        historyTopicHandler.before(HistoryTopicHandler.HAction.RERENDER)

        val onResultProxy = object : OnTransferResult {
            override fun onResult(result: Transfer) {
                transferring.remove(transfer)
                historyTopicHandler.before(HistoryTopicHandler.HAction.RERENDER)
                onTransferResult?.onResult(result)
            }
        }

        try {
            if (type === TransferType.UPLOAD) {
                if (!localFile.exists()) {
                    val e = "Can't find local file $localFileAbsPath"
                    Services.message(e, MessageType.ERROR)
                    throw TransferException(e)
                }
            } else {
                if (!remoteFile.exists()) {
                    val e = "Can't find remote file $remoteFilePath"
                    Services.message(e, MessageType.ERROR)
                    throw TransferException(e)
                }
            }

            ProgressManager.getInstance().run(object : Task.Backgroundable(
                project,
                if (type === TransferType.UPLOAD) "Uploading" else "Downloading",
            ) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate =
                        if (type === TransferType.UPLOAD) localFile.isDirectory else remoteFile.isDir()
                    try {
                        val fileTransfer = SFTPFileTransfer(sftpClient!!.sftpEngine)
                        fileTransfer.transferListener = object : TransferListener {
                            override fun directory(name: String): TransferListener {
                                return this
                            }
                            override fun file(name: String, size: Long): StreamCopier.Listener {
                                val total: String = FileTableModel.byteCountToDisplaySize(size)
                                return StreamCopier.Listener { transferred: Long ->
                                    if (indicator.isCanceled || transfer.result === TransferResult.CANCELLED) {
                                        throw TransferCancelledException("Operation cancelled!")
                                    }
                                    transfer.transferred = transferred
                                    historyTopicHandler.before(HistoryTopicHandler.HAction.RERENDER)
                                    indicator.text2 = "${(if (type === TransferType.UPLOAD) "Uploading" else "Downloading")} " +
                                            "${transfer.source} to ${transfer.target}"
                                    val percent = transferred.toDouble() / size

                                    // 文件夹的不显示百分比进度
                                    if (!indicator.isIndeterminate) {
                                        indicator.fraction = percent
                                    }
                                    indicator.text = "${((percent * 10000).roundToInt() / 100)}% " +
                                            "${FileTableModel.byteCountToDisplaySize(transferred)}/$total"
                                }
                            }
                        }

                        // 开始传输
                        if (type === TransferType.UPLOAD) {
                            fileTransfer.upload(localFileAbsPath, remoteFilePath)
                        } else {
                            fileTransfer.download(remoteFilePath, localFileAbsPath)
                        }

                        // 如果上传目录和当前目录相同, 则刷新目录
                        if (type === TransferType.UPLOAD) {
                            val currentRemotePath: String = remotePath.getMemoItem() ?: ""
                            if (
                                getParentFolderPath(remoteFilePath, SERVER_FILE_SYSTEM_SEPARATOR)
                                ==
                                currentRemotePath
                            ) {
                                reloadRemote()
                            }
                        } else {
                            val currentLocalPath: String = localPath.getMemoItem() ?: ""
                            if (
                                getParentFolderPath(localFileAbsPath, File.separator)
                                ==
                                currentLocalPath
                            ) {
                                reloadLocal()
                            }
                        }

                        transfer.result = TransferResult.SUCCESS
                        onResultProxy.onResult(transfer)
                    } catch (e: TransferCancelledException) {
                        transfer.result = TransferResult.CANCELLED
                        transfer.exception = e.message ?: "cancelled"
                        onResultProxy.onResult(transfer)
                    } catch (e: Exception) {
                        transfer.result = TransferResult.FAIL
                        transfer.exception = e.message ?: "failed"
                        onResultProxy.onResult(transfer)

                        e.printStackTrace()
                        Services.message(
                            "Error occurred while transferring ${transfer.source} to ${transfer.target}, ${e.message}",
                            MessageType.ERROR,
                        )
                        if (e is SocketException) {
                            disconnect(true)
                        }
                    }
                }
                override fun onCancel() {
                    super.onCancel()
                    this.cancelText = "Cancelling..."
                }
            })
        } catch (e: Exception) {
            transfer.result = TransferResult.FAIL
            transfer.exception = e.message ?: "transfer failed"
            onResultProxy.onResult(transfer)

            e.printStackTrace()
        }
    }

    // endregion

    // region getter / setter

    fun setContent(content: Content?) {
        if (content != null) {
            this.content = content
            ExplorerWindowTabCloseListener(this.content, project, this)
        }
    }

    fun getSftpClient(): SFTPClient? {
        return sftpClient
    }

    // endregion
}

class TransferException(message: String): RuntimeException(message)
class TransferCancelledException(message: String): RuntimeException(message)
