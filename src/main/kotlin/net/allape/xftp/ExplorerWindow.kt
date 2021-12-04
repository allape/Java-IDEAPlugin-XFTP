package net.allape.xftp

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
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
import net.allape.common.RemoteDataProducerWrapper
import net.allape.common.Services
import net.allape.common.Windows
import net.allape.component.FileTable
import net.allape.model.*
import net.allape.util.Maps
import net.schmizz.sshj.sftp.SFTPClient
import java.awt.Desktop
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.util.*
import java.util.stream.Collectors
import javax.swing.DropMode
import javax.swing.JComponent
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class ExplorerWindow(
    project: Project,
    toolWindow: ToolWindow,
) : ExplorerWindowUI(project, toolWindow) {

    // 上一次选择文件
    private var lastFile: FileModel? = null

    // 当前选中的本地文件
    private var currentLocalFile: FileModel? = null

    // 当前选中的远程文件
    private var currentRemoteFile: FileModel? = null

    // 修改中的远程文件, 用于文件修改后自动上传 key: remote file, value: local file
    private val remoteEditingFiles: HashMap<RemoteFileObject, String> = HashMap(COLLECTION_SIZE)

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

    override fun buildUI() {
        super.buildUI()

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

        remotePath.isEnabled = false
        remotePath.addActionListener {
            val remoteFilePath = getCurrentRemotePath()

            if (remoteFilePath.isEmpty()) return@addActionListener

            application.executeOnPooledThread {
                if (sftpChannel == null) {
                    Services.message("Please connect to server first!", MessageType.INFO)
                } else if (!sftpChannel!!.isConnected) {
                    Services.message("SFTP lost connection, retrying...", MessageType.ERROR)
                    connect()
                }
                remoteFileList.isEnabled = false
                remotePath.isEnabled = false
                // 是否接下来加载父文件夹
                var loadParent: String? = null
                try {
                    var path = remoteFilePath.ifEmpty { FILE_SEP }
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
                    remoteFileList.isEnabled = true
                    remotePath.isEnabled = true
                    if (loadParent != null) {
                        // 加载父文件夹
                        setCurrentRemotePath(loadParent)
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


        val remoteFileListPopupMenuDelete = JBMenuItem("rm -Rf ...")
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

    override fun disconnect(triggerEvent: Boolean) {
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

    override fun isChannelAvailable (): Boolean {
        try {
            val file = this.sftpChannel?.file("/dev/null")
            if (file != null) {
                return true
            }
        } catch (e: Exception) {
            disconnect()
        }
        return false
    }

    override fun triggerConnecting() {
        application.invokeLater { explore.setEnabled(false) }
    }

    override fun triggerConnected() {
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

    override fun triggerDisconnected() {
        application.invokeLater {
            // 设置远程路径输入框
            remotePath.item = null
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
        val fireEventManually = currentLocalPath == getCurrentLocalPath()
        localPath.push(currentLocalPath)
        if (fireEventManually) {
            localPath.actionPerformed(null)
        }
    }

    override fun getCurrentLocalPath(defaultValue: String?): String = localPath.getMemoItem(defaultValue) ?: ""

    override fun reloadLocal() {
        setCurrentLocalPath(getCurrentLocalPath())
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

    override fun getCurrentRemotePath(defaultValue: String?): String  = remotePath.getMemoItem(defaultValue) ?: ""

    override fun reloadRemote() {
        setCurrentRemotePath(getCurrentRemotePath())
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
                    transfer(localFile, remoteFile, TransferType.DOWNLOAD) {
                        if (it.result == TransferResult.SUCCESS) {
                            openFileInEditor(localFile)
                            // 加入文件监听队列
                            remoteEditingFiles[remoteFile] = localFile.absolutePath
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Services.message("Unable to create cache file: " + e.message, MessageType.ERROR)
            }
        }
    }

}
