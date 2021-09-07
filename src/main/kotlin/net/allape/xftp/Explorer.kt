package net.allape.xftp

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindow
import com.intellij.remote.RemoteConnectionType
import com.intellij.remote.RemoteCredentials
import com.intellij.ssh.ConnectionBuilder
import com.intellij.ssh.RemoteFileObject
import com.intellij.ssh.SshTransportException
import com.intellij.ssh.channels.SftpChannel
import com.intellij.ssh.connectionBuilder
import com.intellij.ssh.impl.sshj.channels.SshjSftpChannel
import com.intellij.ui.JBSplitter
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.util.ReflectionUtil
import com.jetbrains.plugins.remotesdk.console.RemoteDataProducer
import com.jetbrains.plugins.remotesdk.console.SshTerminalDirectRunner
import icons.TerminalIcons
import net.allape.action.ActionToolbarFastEnableAnAction
import net.allape.common.HistoryTopicHandler
import net.allape.common.Services
import net.allape.common.Windows
import net.allape.component.FileTable
import net.allape.component.MemoComboBox
import net.allape.model.*
import net.schmizz.sshj.sftp.SFTPClient
import org.jetbrains.plugins.terminal.TerminalTabState
import org.jetbrains.plugins.terminal.TerminalView
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.nio.charset.Charset
import java.util.stream.Collectors
import javax.swing.JComponent
import javax.swing.JPanel

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

        val X0Y0: GridBagConstraints = defaultConfig();
        init {
            X0Y0.gridx = 0;
            X0Y0.gridy = 0;
        }

        val X0Y1: GridBagConstraints = defaultConfig();
        init {
            X0Y1.gridx = 0;
            X0Y1.gridy = 1;
        }

        val X1Y0: GridBagConstraints = defaultConfig();
        init {
            X1Y0.gridx = 1;
            X1Y0.gridy = 0;
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

open class Explorer(
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
    private val lastFile: FileModel? = null

    // 当前选中的本地文件
    private val currentLocalFile: FileModel? = null

    // 当前选中的远程文件
    private val currentRemoteFile: FileModel? = null

    // 修改中的远程文件, 用于文件修改后自动上传 key: remote file, value: local file
    private val remoteEditingFiles: HashMap<RemoteFileObject, String> = HashMap(COLLECTION_SIZE)

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

    }

    /**
     * 使用当前选择的配置进行连接
     */
    fun connectSftp() {
        try {
            RemoteDataProducer()
                .withProject(project)
                .produceRemoteData(
                    RemoteConnectionType.SSH_CONFIG,
                    null,
                    "from xftp"
                ) { data ->
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
                                "Connecting to " + getCredentialsName(),
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
                                            val sftpChannelClass = SshjSftpChannel::class.java
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
    open fun disconnect(triggerEvent: Boolean) {
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
    open fun triggerConnecting() {
        application.invokeLater { explore.setEnabled(false) }
    }

    /**
     * 设置当前状态为已连接
     */
    open fun triggerConnected() {
        application.invokeLater {
            remotePath.isEnabled = true
            this.setRemoteButtonsEnable(true)
            if (credentials != null) {
                content.displayName = getCredentialsName()
//                credentials!!.userName +
//                        "@" +
//                        credentials!!.host
            }
        }
    }

    /**
     * 设置当前状态为未连接
     */
    open fun triggerDisconnected() {
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
    open fun setCurrentLocalPath(currentLocalPath: String) {
        val fireEventManually = currentLocalPath == localPath.getMemoItem()
        localPath.push(currentLocalPath)
        if (fireEventManually) {
            localPath.actionPerformed(null)
        }
    }

    /**
     * 刷新本地资源
     */
    open fun reloadLocal() {
        setCurrentLocalPath(localPath.getMemoItem()!!)
    }

    /**
     * 谁当前显示的远程路径
     * @param currentRemotePath 远程文件路径
     */
    open fun setCurrentRemotePath(currentRemotePath: String) {
        val fireEventManually = currentRemotePath == remotePath.getMemoItem()
        remotePath.push(currentRemotePath)
        if (fireEventManually) {
            remotePath.actionPerformed(null)
        }
    }

    /**
     * 刷新远程资源
     */
    open fun reloadRemote() {
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
        val files: MutableList<FileModel> = java.util.ArrayList(rows.size)
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
    private fun getCredentialsName(): String {
        // credentials!!.userName + "@" + credentials!!.host
        return credentials?.toString() ?: Windows.WINDOW_DEFAULT_NAME
    }

    // region 上传下载

    @Synchronized
    private fun transfer(
        localFile: File,
        remoteFile: RemoteFileObject,
        type: TransferType,
        onResult: OnTransferResult,
    ) {
        if (sftpChannel == null || !sftpChannel!!.isConnected) {
            Services.message("Please start a sftp session first!", MessageType.INFO)
            throw IllegalStateException("No SFTP session available!")
        }

        // 格式化远程路径
        val normalizedRemotePath = sftpChannel!!.sshSession.host + ":" + normalizeRemoteFileObjectPath(remoteFile)
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

        // FIXME: 2021/9/7 使用kotlin routine代替rxjava和thread 
        
        if (type === TransferType.UPLOAD) {
            if (!localFile.exists()) {
                val e = "Can't find local file $localFileAbsPath"
                Services.message(e, MessageType.ERROR)
                return
            }
        } else {
            if (!remoteFile.exists()) {
                val e = "Can't find remote file $remoteFilePath"
                Services.message(e, MessageType.ERROR)
                return
            }
        }
    }

    // endregion

    // region getter / setter

    open fun setContent(content: Content?) {
        if (content != null) {
            this.content = content
            ExplorerWindowTabCloseListener(this.content, project, this)
        }
    }

    open fun getSftpClient(): SFTPClient? {
        return sftpClient
    }

    // endregion
}
