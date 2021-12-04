package net.allape.xftp

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ssh.RemoteFileObject
import com.intellij.ssh.SshTransportException
import com.intellij.ssh.connectionBuilder
import com.intellij.util.ReflectionUtil
import com.jetbrains.plugins.remotesdk.console.SshConfigConnector
import com.jetbrains.plugins.remotesdk.console.SshTerminalDirectRunner
import icons.TerminalIcons
import net.allape.action.ActionToolbarFastEnableAnAction
import net.allape.common.RemoteDataProducerWrapper
import net.allape.common.Services
import net.allape.component.FileNameTextFieldDialog
import net.allape.model.FileModel
import net.allape.model.FileTransferHandler
import net.allape.model.FileTransferable
import net.allape.model.TransferType
import net.allape.util.Maps
import net.schmizz.sshj.sftp.SFTPClient
import org.jetbrains.plugins.terminal.TerminalTabState
import org.jetbrains.plugins.terminal.TerminalView
import java.awt.Desktop
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.Future
import java.util.stream.Collectors
import javax.swing.DropMode
import javax.swing.JComponent
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class ExplorerWindow(
    project: Project,
    toolWindow: ToolWindow,
) : ExplorerWindowUI(project, toolWindow) {

    companion object {
        // 双击间隔, 毫秒
        const val DOUBLE_CLICK_INTERVAL: Long = 350
    }

    private var clickWatcher: Long = System.currentTimeMillis()

    // 上一次选择文件
    private var lastFile: FileModel? = null

    // 当前选中的本地文件
    private var currentLocalFile: FileModel? = null

    // 当前选中的远程文件
    private var currentRemoteFile: FileModel? = null

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

        buildUI()
    }

    override fun dispose() {
        super.dispose()
        // 关闭连接
        disconnect()
    }

    override fun bindLocalIU() {
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
                    getCurrentLocalPath().let { path ->
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
            var path = getCurrentLocalPath().ifEmpty { File.separator }
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
                val fileModels: List<FileModel> = localFileList.selected()
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
                    getCurrentLocalPath().let { currentLocalPath ->
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
    }

    override fun bindRemoteUI() {
        buildRemoteListActionButtons()

        remotePath.isEnabled = false
        remotePath.addActionListener {
            val remoteFilePath = getCurrentRemotePath()

            if (remoteFilePath.isEmpty()) return@addActionListener

            application.executeOnPooledThread {
                if (this.isChannelAlive()) {
                    lockRemoteUIs()
                    // 是否接下来加载父文件夹
                    var loadParent: String? = null
                    try {
                        var path = remoteFilePath.ifEmpty { FILE_SEP }
                        val file = sftpChannel!!.file(path)
                        path = file.path()
                        if (!file.exists()) {
                            Services.message("$path does not exist!", MessageType.INFO)
                            loadParent = getParentFolderPath(file.path(), FILE_SEP)
                        } else if (!file.isDir()) {
                            downloadFileAndEdit(file)
                            loadParent = getParentFolderPath(file.path(), FILE_SEP)
                        } else {
                            val files = file.list()
                            val fileModels: MutableList<FileModel> =
                                ArrayList(files.size)

                            // 添加返回上一级目录
                            fileModels.add(getParentFolder(path, FILE_SEP))
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
                        unlockRemoteUIs()
                        if (loadParent != null) {
                            // 加载父文件夹
                            setCurrentRemotePath(loadParent)
                        }
                    }
                }
            }
        }
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
                    getCurrentRemotePath().let { currentRemotePath ->
                        for (file in files) {
                            application.executeOnPooledThread {
                                transfer(
                                    file,
                                    sftpChannel!!.file(currentRemotePath + FILE_SEP + file.name),
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

        buildRemoteListContextMenu()
    }

    /**
     * 构建远程列表左侧action按钮
     */
    private fun buildRemoteListActionButtons() {
        explore = object : ActionToolbarFastEnableAnAction(
            remoteActionToolBar,
            "Start New Session", "Start a sftp session",
            AllIcons.Webreferences.Server
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                connect()
            }
        }
        dropdown = object : ActionToolbarFastEnableAnAction(
            remoteActionToolBar,
            "Dropdown", "Display remote access history",
            AllIcons.Actions.MoveDown
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                remotePath.isPopupVisible = true
            }
        }
        reload = object : ActionToolbarFastEnableAnAction(
            remoteActionToolBar,
            "Reload Remote", "Reload current remote folder",
            AllIcons.Actions.Refresh
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                reloadRemote()
            }
        }
        suspend = object : ActionToolbarFastEnableAnAction(
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
                    disconnect()
                }
            }
        }
        newTerminal = object : ActionToolbarFastEnableAnAction(
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
        localToggle = object : ActionToolbarFastEnableAnAction(
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
        // 远程列表窗口组件
        remoteActionGroup.addAll(
            explore,
            dropdown,
            reload,
            suspend,
            newTerminal,
            Separator.create(),
            localToggle
        )
    }

    /**
     * 构建远程列表右键菜单
     */
    private fun buildRemoteListContextMenu() {
        rmRf.addActionListener {
            val files: List<RemoteFileObject> = getSelectedRemoteFileList()
            if (MessageDialogBuilder.yesNo(
                    "Delete Confirm",
                    "${files.joinToString(separator = "\n") { it.name() }}\n\nThis operation is irreversible, continue?"
                )
                .asWarning()
                .yesText("Continue")
                .noText("Cancel")
                .ask(project)
            ) {
                application.invokeLater {
                    lockRemoteUIs()
                    application.executeOnPooledThread {
                        // 开启一个ExecChannel来删除非空的文件夹
                        try {
                            for (file in files) {
                                if (file.exists()) {
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
        touch.addActionListener {
            FileNameTextFieldDialog(project).openDialog(sftpChannel!!) {
                println(it)
            }
        }

        remoteFileListPopupMenu.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                if (isConnected()) {
                    setRemoteListContentMenuItems(true)
                    val selectedFiles = remoteFileList.selected()
                    rmRf.isEnabled = selectedFiles.isNotEmpty()
                    if (rmRf.isEnabled) {
                        rmRf.text = "$RM_RF_TEXT ${selectedFiles[0].name} ${if (selectedFiles.size > 1) "and ${selectedFiles.size-1} more" else ""}"
                    }
                } else {
                    setRemoteListContentMenuItems(false)
                }
            }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
                resetRemoteListContentMenuItemsText()
            }
            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        })
    }

    override fun connect() {
        try {
            RemoteDataProducerWrapper()
                .withProject(project)
                .produceRemoteDataWithConnector(
                    null,
                    null
                ) { c ->
                    c?.let { connector ->
                        this@ExplorerWindow.connector = connector as SshConfigConnector
                        connector.produceRemoteCredentials { data ->
                            if (data != null) {
                                this.triggerConnecting()
                                this.disconnect(false)
                                credentials = data

                                var connectionThread: Future<*>? = null
                                connectionThread = this.application.executeOnPooledThread {
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
                                        true
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
                                            connectionThread?.cancel(true)
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

    override fun disconnect(triggerEvent: Boolean) {
        super.disconnect(triggerEvent)
        if (triggerEvent) {
            triggerDisconnected()
        }
    }

    override fun triggerConnecting() {
        application.invokeLater { explore.setEnabled(false) }
    }

    override fun triggerConnected() {
        application.invokeLater {
            setRemoteButtonsEnable(true)
            unlockRemoteUIs()
            if (credentials != null) {
                content.displayName = getWindowName()
            }
        }
    }

    override fun triggerDisconnected() {
        application.invokeLater {
            // 设置远程路径输入框
            remotePath.item = null
            lockRemoteUIs()
            setRemoteButtonsEnable(false)
            setRemoteListContentMenuItems(false)

            // 清空列表
            remoteFileList.model.resetData(emptyList())
        }
    }

    override fun getCurrentLocalPath(defaultValue: String?): String = localPath.getMemoItem(defaultValue) ?: ""

    override fun reloadLocal() {
        setCurrentLocalPath(getCurrentLocalPath())
    }

    override fun getCurrentRemotePath(defaultValue: String?): String  = remotePath.getMemoItem(defaultValue) ?: ""

    override fun reloadRemote() {
        setCurrentRemotePath(getCurrentRemotePath())
    }

    /**
     * 设置当前显示的本地路径
     * @param currentLocalPath 本地文件路径
     */
    fun setCurrentLocalPath(currentLocalPath: String) {
        val fireEventManually = currentLocalPath == getCurrentLocalPath()
        localPath.push(currentLocalPath)
        if (fireEventManually) {
            localPath.actionPerformed(null)
        }
    }

    /**
     * 设置当前显示的远程路径
     * @param currentRemotePath 远程文件路径
     */
    fun setCurrentRemotePath(currentRemotePath: String) {
        val fireEventManually = currentRemotePath == getCurrentRemotePath()
        remotePath.push(currentRemotePath)
        if (fireEventManually) {
            remotePath.actionPerformed(null)
        }
    }

    /**
     * 获取当前选中了的远程文件列表
     * @return 远程文件列表
     */
    private fun getSelectedRemoteFileList(): List<RemoteFileObject> {
        try {
            lockRemoteUIs()
            return remoteFileList.selected()
                .stream()
                .map{ (path): FileModel ->
                    sftpChannel!!.file(
                        path
                    )
                }
                .collect(Collectors.toList())
        } finally {
            unlockRemoteUIs()
        }
    }

}
