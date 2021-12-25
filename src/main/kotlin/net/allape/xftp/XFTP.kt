package net.allape.xftp

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.remote.RemoteCredentials
import com.intellij.ssh.RemoteFileObject
import com.intellij.ssh.SshTransportException
import com.intellij.ssh.connectionBuilder
import com.intellij.util.Consumer
import com.intellij.util.ReflectionUtil
import com.jetbrains.plugins.remotesdk.console.SshConfigConnector
import net.allape.action.Actions
import net.allape.common.RemoteDataProducerWrapper
import net.allape.common.XFTPManager
import net.allape.model.FileModel
import net.allape.model.FileTransferHandler
import net.allape.model.FileTransferable
import net.allape.model.TransferType
import net.allape.util.LinuxHelper
import net.allape.util.Maps
import net.allape.xftp.component.FileNameTextFieldDialog
import net.schmizz.sshj.sftp.SFTPClient
import java.awt.Desktop
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.util.concurrent.Future
import java.util.stream.Collectors
import javax.swing.DropMode
import javax.swing.JComponent
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class XFTP(
    project: Project,
    toolWindow: ToolWindow,
) : XFTPWidget(project, toolWindow) {

    companion object {
        // 双击间隔, 毫秒
        const val DOUBLE_CLICK_INTERVAL: Long = 350
        // 放入content.userData的key
        val XFTP_KEY: Key<XFTP> = Key.create(XFTP::javaClass.name)
    }

    private val logger = Logger.getInstance(XFTP::class.java)

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
        localPath.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                localFileList.clearSelection()
                remoteFileList.clearSelection()
            }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                fetchLocalList(localPath.getMemoItem() ?: FILE_SEP)
            }
            override fun popupMenuCanceled(e: PopupMenuEvent?) {}
        })
        localPath.addActionListener {
            if (it.actionCommand == "comboBoxEdited") {
                fetchRemoteList(localPath.getMemoItem() ?: FILE_SEP)
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
        remotePath.isEnabled = false
        remotePath.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                localFileList.clearSelection()
                remoteFileList.clearSelection()
            }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                fetchRemoteList(remotePath.getMemoItem() ?: FILE_SEP)
            }
            override fun popupMenuCanceled(e: PopupMenuEvent?) {}
        })
        remotePath.addActionListener {
            if (it.actionCommand == "comboBoxEdited") {
                fetchRemoteList(remotePath.getMemoItem() ?: FILE_SEP)
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
     * 构建远程列表右键菜单
     */
    private fun buildRemoteListContextMenu() {
        cdDotDot.let {
            it.addActionListener {
                if (isConnected() && isChannelAlive()) {
                    setCurrentRemotePath(remoteFileList.model.data[0].path)
                }
            }
            it.accelerator = Actions.getActionFirstKeyStroke(Actions.GoUpperAction)
        }
        cdOrCat.let {
            it.addActionListener {
                if (isConnected() && isChannelAlive()) {
                    getSelectedRemoteFileList().takeIf { l -> l.isNotEmpty() }?.let { selectedFiles ->
                        setCurrentRemotePath(selectedFiles[0].path())
                    }
                }
            }
            it.accelerator = Actions.getActionFirstKeyStroke(Actions.OpenAction)
        }
        rmRf.let { rmRf ->
            rmRf.addActionListener {
                val files: List<RemoteFileObject> = getSelectedRemoteFileList()
                if (files.isNotEmpty() && isConnected() && isChannelAlive()) {
                    if (MessageDialogBuilder.yesNo(
                            "Delete Confirm",
                            "${files.joinToString(separator = "\n") { it.name() }}\n\n${files.size} item${if (files.size > 1) "s" else ""} will be deleted."
                        )
                            .asWarning()
                            .yesText("Delete")
                            .noText("Cancel")
                            .ask(project)
                    ) {
                        executeOnPooledThreadWithRemoteUILocked({
                            // 开启一个ExecChannel来删除非空的文件夹
                            for (file in files) {
                                if (file.exists()) {
                                    try {
                                        executeSync("rm -Rf ${LinuxHelper.escapeShellString(file.path())}")
                                    } catch (err: java.lang.Exception) {
                                        err.printStackTrace()
                                        XFTPManager.message(
                                            "Error occurred while deleting file: ${file.path()}",
                                            MessageType.ERROR
                                        )
                                    }
                                }
                            }

                            true
                        })
                    }
                }
            }
            rmRf.accelerator = Actions.getActionFirstKeyStroke(Actions.DeleteAction)
        }
        duplicate.let {
            it.addActionListener {
                val files: List<RemoteFileObject> = getSelectedRemoteFileList()

                if (files.size == 1 && isConnected() && isChannelAlive()) {
                    val file = files[0]
                    val isDir = file.isDir()

                    FileNameTextFieldDialog(project).openDialog(isDir, file.name()) { filename ->
                        executeOnPooledThreadWithRemoteUILocked({
                            val parsedFileName = joinWithCurrentRemotePath(filename.trim())

                            val folderName = getParentFolderPath(parsedFileName, FILE_SEP)
                            if (folderName != FILE_SEP) {
                                sftpClient!!.mkdirs(folderName)
                            }

                            executeSync("cp${if (isDir) " -R" else ""} ${LinuxHelper.escapeShellString(file.path())} ${LinuxHelper.escapeShellString(parsedFileName)}")

                            setCurrentRemotePath(parsedFileName)

                            false
                        }) {
                            XFTPManager.message(
                                "Error occurred while duplicating file: ${it.message}",
                                MessageType.ERROR
                            )
                        }
                    }
                }
            }
            it.accelerator = Actions.getActionFirstKeyStroke(Actions.DuplicateAction)
        }
        mv.let {
            it.addActionListener {
                val files: List<RemoteFileObject> = getSelectedRemoteFileList()

                if (files.size == 1 && isConnected() && isChannelAlive()) {
                    val file = files[0]
                    val isDir = file.isDir()

                    FileNameTextFieldDialog(project).openDialog(isDir, file.name()) { filename ->
                        executeOnPooledThreadWithRemoteUILocked({
                            val parsedFileName = joinWithCurrentRemotePath(filename.trim())

                            val folderName = getParentFolderPath(parsedFileName, FILE_SEP)
                            if (folderName != FILE_SEP) {
                                sftpClient!!.mkdirs(folderName)
                            }

                            executeSync("mv ${LinuxHelper.escapeShellString(file.path())} ${LinuxHelper.escapeShellString(parsedFileName)}")

                            setCurrentRemotePath(parsedFileName)

                            false
                        }) {
                            XFTPManager.message(
                                "Error occurred while renaming file: ${it.message}",
                                MessageType.ERROR
                            )
                        }
                    }
                }
            }
            it.accelerator = Actions.getActionFirstKeyStroke(Actions.RenameAction)
        }
        touch.let {
            it.addActionListener {
                if (isConnected() && isChannelAlive()) {
                    FileNameTextFieldDialog(project).openDialog(false) { filename ->
                        executeOnPooledThreadWithRemoteUILocked({
                            val parsedFileName = joinWithCurrentRemotePath(filename.trim())

                            val folderName = getParentFolderPath(parsedFileName, FILE_SEP)
                            if (folderName != FILE_SEP) {
                                sftpClient!!.mkdirs(folderName)
                            }

                            executeSync("touch ${LinuxHelper.escapeShellString(parsedFileName)}")
                            // 打开当前文件
                            setCurrentRemotePath(parsedFileName)

                            false
                        }) { ex ->
                            XFTPManager.message(
                                "Error occurred while creating file: ${ex.message}",
                                MessageType.ERROR
                            )
                        }
                    }
                }
            }
            it.accelerator = Actions.getActionFirstKeyStroke(Actions.NewFileAction)
        }
        mkdirp.let {
            it.addActionListener {
                if (isConnected() && isChannelAlive()) {
                    FileNameTextFieldDialog(project).openDialog(true) { filename ->
                        executeOnPooledThreadWithRemoteUILocked({
                            val fullPath = joinWithCurrentRemotePath(filename.trim())
                            sftpClient!!.mkdirs(fullPath)

                            setCurrentRemotePath(fullPath)

                            false
                        }) {
                            XFTPManager.message(
                                "Error occurred while creating folder: ${it.message}",
                                MessageType.ERROR
                            )
                        }
                    }
                }
            }
            it.accelerator = Actions.getActionFirstKeyStroke(Actions.NewFolderAction)
        }

        remoteFileListPopupMenu.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                if (isConnected()) {
                    setRemoteListContentMenuItems(true)

                    val selectedFiles = remoteFileList.selected()
                    val hasFileSelected = selectedFiles.isNotEmpty()

                    rmRf.isEnabled = hasFileSelected
                    if (rmRf.isEnabled) {
                        rmRf.text = "$RM_RF_TEXT ${LinuxHelper.escapeShellString(selectedFiles[0].name)}${if (selectedFiles.size > 1) " # and ${selectedFiles.size-1} more" else ""}"
                    }

                    duplicate.isEnabled = hasFileSelected
                    if (duplicate.isEnabled) {
                        duplicate.text = "$CP_TEXT${if (selectedFiles[0].directory) " -R" else ""} ${LinuxHelper.escapeShellString(selectedFiles[0].name)} \$1"
                    }
                    mv.isEnabled = hasFileSelected
                    if (mv.isEnabled) {
                        mv.text = "$MV_TEXT ${LinuxHelper.escapeShellString(selectedFiles[0].name)} $1"
                    }

                    cdOrCat.isEnabled = hasFileSelected
                    if (cdOrCat.isEnabled) {
                        val file = selectedFiles[0]
                        if (file.directory) {
                            cdOrCat.text = "$CD_TEXT ${LinuxHelper.escapeShellString(file.name)}"
                        } else {
                            cdOrCat.text = "$CAT_TEXT ${LinuxHelper.escapeShellString(file.name)}"
                        }
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

    override fun connect(onServerSelect: Consumer<RemoteCredentials>?) {
        try {
            RemoteDataProducerWrapper()
                .withProject(project)
                .produceRemoteDataWithConnector(
                    null,
                    null
                ) { c ->
                    c?.let { connector ->
                        this@XFTP.connector = connector as SshConfigConnector
                        connector.produceRemoteCredentials { data ->
                            if (data != null) {
                                this.triggerConnecting()
                                this.disconnect(false)
                                credentials = data

                                onServerSelect?.consume(data)

                                var connectionThread: Future<*>? = null
                                connectionThread = this.application.executeOnPooledThread {
                                    // com.jetbrains.plugins.remotesdk.tools.RemoteTool.startRemoteProcess
                                    @Suppress("UnstableApiUsage")
                                    connectionBuilder = credentials!!.connectionBuilder(
                                        project,
                                        ProgressManager.getGlobalProgressIndicator(),
                                        true
                                    ).withConnectionTimeout(30L)

                                    val connectionLogName = "${credentials!!.userName}@${credentials!!.host}:${credentials!!.port}"
                                    logger.info("Start to connect to $connectionLogName...")

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

                                                logger.info("Connection established with $connectionLogName")

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
                                                    XFTPManager.message(
                                                        "Failed to get sftp client for this session, please try it again: ${e.message}",
                                                        MessageType.ERROR
                                                    )
                                                }

                                                application.invokeLater { setCurrentRemotePath(sftpChannel!!.home) }
                                            } catch (e: SshTransportException) {
                                                if (!cancelled) {
                                                    XFTPManager.message("Failed to connect: " + e.message, MessageType.WARNING)
                                                }
                                                triggerDisconnected()
                                                e.printStackTrace()
                                            }
                                        }

                                        override fun onCancel() {
                                            super.onCancel()
                                            cancelled = true
                                            connectionThread?.cancel(true)
                                            logger.info("Cancelling connection of $connectionLogName...")
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
            XFTPManager.message(
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
        application.invokeLater { explore.enabled = false }
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
        localPath.push(currentLocalPath)
        fetchLocalList(currentLocalPath)
    }

    /**
     * 设置当前显示的远程路径
     * @param currentRemotePath 远程文件路径
     */
    fun setCurrentRemotePath(currentRemotePath: String) {
        remotePath.push(currentRemotePath)
        fetchRemoteList(currentRemotePath)
    }

    /**
     * 获取当前选中了的远程文件列表
     * @return 远程文件列表
     */
    fun getSelectedRemoteFileList(): List<RemoteFileObject> {
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

    /**
     * 加载指定路径的本地文件列表
     */
    private fun fetchLocalList(targetPath: String) {
        var path = targetPath.ifEmpty { File.separator }
        var nextDir: String? = null
        try {
            val file = File(path)
            if (!file.exists()) {
                XFTPManager.message("$path does not exist!", MessageType.INFO)
                // 获取第二个历史, 不存在时加载项目路径
                if (project.basePath != null) {
                    nextDir = project.basePath
                } else if (!path.equals(File.separator)) {
                    nextDir = File.separator
                }
            } else if (!file.isDirectory) {
                // 加载父文件夹
                nextDir = file.parent
                if (file.length() > EDITABLE_FILE_SIZE) {
                    if (MessageDialogBuilder.yesNo(
                            "This file is too large for text editor",
                            "Do you still want to edit it?"
                        )
                            .asWarning()
                            .yesText("Edit")
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
                            XFTPManager.message(
                                "Failed to open file in system file manager",
                                MessageType.INFO
                            )
                        }
                    }
                    return
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
            XFTPManager.message(ex.message!!, MessageType.WARNING)
        } finally {
            if (nextDir != null && nextDir != path) {
                setCurrentLocalPath(nextDir)
            }
        }
    }

    /**
     * 加载指定路径的远程目录列表
     */
    private fun fetchRemoteList(targetPath: String) {
        if (targetPath.isEmpty()) return
        application.executeOnPooledThread {
            if (this.isChannelAlive()) {
                lockRemoteUIs()
                // 是否接下来加载父文件夹
                var loadParent: String? = null
                try {
                    var path = targetPath.ifEmpty { FILE_SEP }
                    val file = sftpChannel!!.file(path)
                    path = file.path()
                    if (!file.exists()) {
                        XFTPManager.message("$path does not exist!", MessageType.INFO)
                        loadParent = getParentFolderPath(file.path(), FILE_SEP)
                    } else if (!file.isDir()) {
                        downloadFileAndEdit(file)
                        loadParent = getParentFolderPath(file.path(), FILE_SEP)
                    } else {
                        val files = file.list()
                        val fileModels: MutableList<FileModel> =
                            ArrayList(files.size)

                        val shortcutsForParentFolder = Actions.getActionFirstKeyStroke(Actions.GoUpperAction)?.toString()
                        // 添加返回上一级目录
                        fileModels.add(getParentFolder(path, FILE_SEP, ".. ${if (shortcutsForParentFolder != null) "(${shortcutsForParentFolder})" else ""}"))
                        for (f in files) {
                            fileModels.add(
                                FileModel(
                                    // 处理有些文件夹是//开头的
                                    normalizeRemoteFileObjectPath(f),
                                    f.name(), f.isDir(), f.size(), f.permissions, false
                                )
                            )
                        }
                        application.invokeLater {
                            remoteFileList.resetData(fileModels)
                            remoteFileList.requestFocusInWindow()
                        }
                    }
                } catch (ex: java.lang.Exception) {
                    ex.printStackTrace()
                    XFTPManager.message(
                        "Error occurred while listing remote files: " + ex.message,
                        MessageType.ERROR
                    )
                } finally {
                    unlockRemoteUIs()
                    if (loadParent != null && loadParent != targetPath) {
                        // 加载父文件夹
                        setCurrentRemotePath(loadParent)
                    }
                }
            }
        }
    }

}
