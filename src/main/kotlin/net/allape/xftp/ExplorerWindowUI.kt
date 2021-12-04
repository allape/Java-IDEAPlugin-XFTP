package net.allape.xftp

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ssh.RemoteFileObject
import com.intellij.ui.JBSplitter
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.plugins.remotesdk.console.SshTerminalDirectRunner
import icons.TerminalIcons
import net.allape.action.ActionToolbarFastEnableAnAction
import net.allape.common.Services
import net.allape.component.FileTable
import net.allape.component.MemoComboBox
import org.jetbrains.plugins.terminal.TerminalTabState
import org.jetbrains.plugins.terminal.TerminalView
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import javax.swing.JComponent
import javax.swing.JPanel

abstract class ExplorerWindowUI(
    project: Project,
    toolWindow: ToolWindow,
) : ExplorerBaseWindow(project, toolWindow, ApplicationManager.getApplication()) {

    // 双击监听
    protected var clickWatcher: Long = System.currentTimeMillis()

    companion object {
        private fun defaultConfig(): GridBagConstraints {
            val gridBagCons = GridBagConstraints()
            gridBagCons.fill = GridBagConstraints.BOTH
            gridBagCons.weightx = 1.0
            gridBagCons.weighty = 1.0
            return gridBagCons
        }

        private val X0Y0: GridBagConstraints = defaultConfig()
        private val X0Y1: GridBagConstraints = defaultConfig()
        private val X1Y0: GridBagConstraints = defaultConfig()

        private val noYWeightX0Y0 = X0Y0.clone() as GridBagConstraints
        private val noXWeightX0Y0 = X0Y0.clone() as GridBagConstraints
        private val noXWeightX1Y0 = X1Y0.clone() as GridBagConstraints

        init {
            X0Y0.gridx = 0
            X0Y0.gridy = 0
            X0Y1.gridx = 0
            X0Y1.gridy = 1
            X1Y0.gridx = 1
            X1Y0.gridy = 0
            noYWeightX0Y0.weighty = 0.0
            noXWeightX0Y0.weightx = 0.0
            noXWeightX1Y0.weightx = 0.0
        }

        // 双击间隔, 毫秒
        const val DOUBLE_CLICK_INTERVAL: Long = 350

        // 远程文件拖拽flavor
        val remoteFileListFlavor = DataFlavor(RemoteFileObject::class.java, "SSH remote file list")

        // region UI配置

        const val LOCAL_HISTORY_PERSISTENCE_KEY = "xftp.persistence.local-history"
        const val REMOTE_HISTORY_PERSISTENCE_KEY = "xftp.persistence.remote-history"

        const val LOCAL_TOOL_BAR_PLACE = "XFTPLocalToolBar"
        const val REMOTE_TOOL_BAR_PLACE = "XFTPRemoteToolBar"

        // endregion
    }

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

    // region actions图标按钮

    // 建立连接
    protected val explore: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
        remoteActionToolBar,
        "Start New Session", "Start a sftp session",
        AllIcons.Webreferences.Server
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            connect()
        }
    }

    // 显示combobox的下拉内容
    protected val dropdown: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
        remoteActionToolBar,
        "Dropdown", "Display remote access history",
        AllIcons.Actions.MoveDown
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            remotePath.isPopupVisible = true
        }
    }

    // 刷新
    protected val reload: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
        remoteActionToolBar,
        "Reload Remote", "Reload current remote folder",
        AllIcons.Actions.Refresh
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            reloadRemote()
        }
    }

    // 断开连接
    protected val suspend: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
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

    // 命令行打开
    protected val newTerminal: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
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
    protected val localToggle: ActionToolbarFastEnableAnAction = object : ActionToolbarFastEnableAnAction(
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

    /**
     * 初始化UI样式
     */
    protected open fun buildUI() {
        this.buildLocalUI()
        this.buildRemoteUI()

        splitter.firstComponent = localWrapper
        splitter.secondComponent = remoteWrapper
        panelWrapper.add(splitter, BorderLayout.CENTER)
        panelWrapper.border = null
    }

    /**
     * 构建本地文件列表
     */
    protected open fun buildLocalUI() {
        localPath.setMinimumAndPreferredWidth(100)
        localPathWrapper.add(localPath, noYWeightX0Y0)
        localFileListWrapper.border = null
        localPathWrapper.add(localFileListWrapper, X0Y1)
        localWrapper.add(localPathWrapper, X0Y0)
        val localActionToolBarWrapper = JPanel(BorderLayout())
        localActionToolBarWrapper.minimumSize = Dimension(48, 0)
        localActionToolBarWrapper.add(localActionToolBar)
        localWrapper.add(localActionToolBarWrapper, noXWeightX1Y0)

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

        localWrapper.border = null
    }

    /**
     * 构建远程文件列表
     */
    protected open fun buildRemoteUI() {
        val remoteActionToolBarWrapper = JPanel(BorderLayout())
        remoteActionToolBarWrapper.minimumSize = Dimension(48, 0)
        remoteActionToolBarWrapper.add(remoteActionToolBar)
        remoteWrapper.add(remoteActionToolBarWrapper, noXWeightX0Y0)
        remotePath.setMinimumAndPreferredWidth(100)
        remotePathWrapper.add(remotePath, noYWeightX0Y0)
        remoteFileListWrapper.border = null
        remotePathWrapper.add(remoteFileListWrapper, X0Y1)
        remoteWrapper.add(remotePathWrapper, X1Y0)

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

        remoteWrapper.border = null
    }

    /**
     * 获取当前window的UI内容
     */
    open fun getUI(): JComponent {
        return panelWrapper
    }

    /**
     * 设置需要进行连接后才能使用的按钮的状态
     * @param enable 是否启用
     */
    protected fun setRemoteButtonsEnable(enable: Boolean) {
        explore.setEnabled(!enable)
        dropdown.setEnabled(enable)
        reload.setEnabled(enable)
        suspend.setEnabled(enable)
        newTerminal.setEnabled(enable)
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