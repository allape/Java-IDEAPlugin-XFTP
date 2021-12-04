package net.allape.xftp

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.remote.RemoteCredentials
import com.intellij.ssh.ConnectionBuilder
import com.intellij.ssh.RemoteFileObject
import com.intellij.ssh.channels.SftpChannel
import com.intellij.ui.content.Content
import com.jetbrains.plugins.remotesdk.console.SshConfigConnector
import net.schmizz.sshj.sftp.SFTPClient
import java.awt.GridBagConstraints
import java.awt.datatransfer.DataFlavor

abstract class SuperExplorer(
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
     * 建立SFTP连接
     */
    abstract fun connect()

    /**
     * 断开SFTP连接
     */
    abstract fun disconnect(triggerEvent: Boolean = true)

    /**
     * 设置当前状态为连接中
     */
    abstract fun triggerConnecting()

    /**
     * 设置当前状态为已连接
     */
    abstract fun triggerConnected()

    /**
     * 设置当前状态为未连接
     */
    abstract fun triggerDisconnected()

    /**
     * 刷新本地目录
     */
    abstract fun reloadLocal()

    /**
     * 刷新远程目录
     */
    abstract fun reloadRemote()

}