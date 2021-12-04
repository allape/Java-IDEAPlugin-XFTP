package net.allape.xftp

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.remote.RemoteCredentials
import com.intellij.ssh.ConnectionBuilder
import com.intellij.ssh.RemoteFileObject
import com.intellij.ssh.channels.SftpChannel
import com.intellij.ui.content.Content
import com.intellij.util.Consumer
import com.jetbrains.plugins.remotesdk.console.SshConfigConnector
import net.allape.common.HistoryTopicHandler
import net.allape.common.Services
import net.allape.common.Windows
import net.allape.component.FileTableModel
import net.allape.model.FileModel
import net.allape.model.Transfer
import net.allape.model.TransferResult
import net.allape.model.TransferType
import net.schmizz.sshj.common.StreamCopier
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.sftp.SFTPFileTransfer
import net.schmizz.sshj.xfer.TransferListener
import java.io.File
import java.io.IOException
import java.net.SocketException
import java.util.HashMap
import kotlin.math.roundToInt

class TransferException(message: String): RuntimeException(message)
class TransferCancelledException(message: String): RuntimeException(message)

abstract class ExplorerBaseWindow(
    protected val project: Project,
    protected val toolWindow: ToolWindow,
) : Disposable {

    protected val application: Application = ApplicationManager.getApplication()

    companion object {
        // 服务器文件系统分隔符
        const val FILE_SEP = "/"

        // 默认集合大小
        const val COLLECTION_SIZE = 100

        // 当前用户本地home目录
        val USER_HOME: String = System.getProperty("user.home")

        // 最大可打开文件
        const val EDITABLE_FILE_SIZE = (2 * 1024 * 1024).toLong()

        /**
         * 格式化远程文件的路径, 因为有的文件开头是//
         * @param file 远程文件信息
         * @return 格式化后的文件路径
         */
        fun normalizeRemoteFileObjectPath(file: RemoteFileObject): String {
            return if (file.path().startsWith("//")) file.path().substring(1) else file.path()
        }

        /**
         * 获取上一级目录路径
         * @param path 当前目录
         * @param sep 文件系统分隔符
         */
        fun getParentFolderPath(path: String, sep: String): String {
            val lastIndexOfSep = path.lastIndexOf(sep)
            val parentFolder = if (lastIndexOfSep == -1 || lastIndexOfSep == 0) "" else path.substring(0, lastIndexOfSep)
            return parentFolder.ifEmpty { sep }
        }

        /**
         * 获取上一级目录的file对象
         * @param path 当前目录
         * @param sep 文件系统分隔符
         */
        fun getParentFolder(path: String, sep: String): FileModel {
            return FileModel(getParentFolderPath(path, sep), "..", true, 0, 0, false)
        }
    }

    // 窗口content
    protected lateinit var content: Content

    // 当前配置的连接器
    protected var connector: SshConfigConnector? = null
    // 当前配置
    protected var credentials: RemoteCredentials? = null
    // 当前配置的连接创建者
    protected var connectionBuilder: ConnectionBuilder? = null
    // 当前开启的channel
    protected var sftpChannel: SftpChannel? = null
    // 当前channel中的sftp client
    var sftpClient: SFTPClient? = null
        protected set

    // 传输历史记录topic的publisher
    protected val historyTopicHandler: HistoryTopicHandler = project.messageBus.syncPublisher(HistoryTopicHandler.HISTORY_TOPIC)

    // 传输历史
    protected val history: ArrayList<Transfer> = ArrayList(COLLECTION_SIZE)

    // 传输中的
    protected val transferring: ArrayList<Transfer> = ArrayList(COLLECTION_SIZE)

    // 修改中的远程文件, 用于文件修改后自动上传 key: remote file, value: local file
    protected val remoteEditingFiles: HashMap<RemoteFileObject, String> = HashMap(COLLECTION_SIZE)

    override fun dispose() {}

    /**
     * 绑定tool window的content
     */
    fun bindContext (content: Content?) {
        content?.let {
            this.content = it
            ExplorerWindowTabCloseListener(it, project, this)
        }
    }

    /**
     * 获取当前连接名称
     */
    fun getWindowName(): String {
        return if (connector != null) connector!!.name
        else if (credentials != null) "${credentials!!.userName}@${credentials!!.host}:${credentials!!.port}"
        else Windows.WINDOW_DEFAULT_NAME
    }

    /**
     * 建立SFTP连接
     */
    abstract fun connect()

    /**
     * 检查当前channel是否可用, 长时间不使用可能会断开连接
     */
    abstract fun isChannelAvailable(): Boolean

    /**
     * 传输文件
     */
    @Synchronized
    fun transfer(
        localFile: File,
        remoteFile: RemoteFileObject,
        type: TransferType,
        resultConsumer: Consumer<Transfer>? = null,
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

        val resultConsumerProxy = Consumer<Transfer> {
            transferring.remove(transfer)
            historyTopicHandler.before(HistoryTopicHandler.HAction.RERENDER)
            resultConsumer?.consume(transfer)
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
                            val currentRemotePath: String = getCurrentRemotePath()
                            if (
                                getParentFolderPath(remoteFilePath, FILE_SEP)
                                ==
                                currentRemotePath
                            ) {
                                reloadRemote()
                            }
                        } else {
                            val currentLocalPath: String = getCurrentLocalPath()
                            if (
                                getParentFolderPath(localFileAbsPath, File.separator)
                                ==
                                currentLocalPath
                            ) {
                                reloadLocal()
                            }
                        }

                        transfer.result = TransferResult.SUCCESS
                        resultConsumerProxy.consume(transfer)
                    } catch (e: TransferCancelledException) {
                        transfer.result = TransferResult.CANCELLED
                        transfer.exception = e.message ?: "cancelled"
                        resultConsumerProxy.consume(transfer)
                    } catch (e: Exception) {
                        transfer.result = TransferResult.FAIL
                        transfer.exception = e.message ?: "failed"
                        resultConsumerProxy.consume(transfer)

                        e.printStackTrace()
                        Services.message(
                            "Error occurred while transferring ${transfer.source} to ${transfer.target}, ${e.message}",
                            MessageType.ERROR,
                        )
                        if (e is SocketException) {
                            disconnect()
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
            resultConsumerProxy.consume(transfer)

            e.printStackTrace()
        }
    }

    /**
     * 断开SFTP连接
     */
    abstract fun disconnect(triggerEvent: Boolean = true)

    /**
     * 设置当前状态为连接中
     */
    protected open fun triggerConnecting() {}

    /**
     * 设置当前状态为已连接
     */
    protected open fun triggerConnected() {}

    /**
     * 设置当前状态为未连接
     */
    protected open fun triggerDisconnected() {}

    /**
     * 刷新本地目录
     */
    abstract fun reloadLocal()

    /**
     * 获取当前本地目录
     */
    abstract fun getCurrentLocalPath(defaultValue: String? = null): String

    /**
     * 刷新远程目录
     */
    abstract fun reloadRemote()

    /**
     * 获取当前远程目录
     */
    abstract fun getCurrentRemotePath(defaultValue: String? = null): String

    /**
     * 打开文件<br/>
     * 参考: [com.intellij.openapi.fileEditor.impl.text.FileDropHandler.openFiles]
     * @param file 文件
     */
    protected fun openFileInEditor(file: File) {
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

    /**
     * 下载文件并编辑
     * @param remoteFile 远程文件信息
     */
    protected fun downloadFileAndEdit(remoteFile: RemoteFileObject) {
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