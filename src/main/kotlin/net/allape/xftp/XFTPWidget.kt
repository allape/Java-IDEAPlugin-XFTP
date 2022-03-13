package net.allape.xftp

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ssh.RemoteFileObject
import com.intellij.ui.JBSplitter
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Consumer
import net.allape.action.Actions
import net.allape.action.MutableAction
import net.allape.action.SimpleMutableAction
import net.allape.common.XFTPManager
import net.allape.xftp.component.FileTable
import net.allape.xftp.component.MemoComboBox
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.event.ActionEvent
import java.io.File
import java.io.IOException
import java.util.function.Supplier
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel

/**
 * 纯UI组件
 */
abstract class XFTPWidget(
    project: Project,
    toolWindow: ToolWindow,
) : XFTPCore(project, toolWindow) {

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

        // 远程文件拖拽flavor
        val remoteFileListFlavor = DataFlavor(RemoteFileObject::class.java, "SSH remote file list")

        // region UI配置

        const val LOCAL_HISTORY_PERSISTENCE_KEY = "xftp.persistence.local-history"
        const val REMOTE_HISTORY_PERSISTENCE_KEY = "xftp.persistence.remote-history"

        const val LOCAL_TOOL_BAR_PLACE = "XFTPLocalToolBar"
        const val REMOTE_TOOL_BAR_PLACE = "XFTPRemoteToolBar"

        // endregion

        const val CAT_TEXT = "cat"
        const val CD_TEXT = "cd"
        const val RM_RF_TEXT = "rm -Rf"
        const val MV_TEXT = "mv"
        const val TOUCH_TEXT = "touch"
        const val CP_TEXT = "cp"
        const val MKDIR_P_TEXT = "mkdir -p"
    }

    // region 来自注册进ActionManager的action

    private val globalReloadLocal: AnAction? = Actions.getActionByNameWithNullSupplier(Actions.ReloadLocalAction)
    private val globalOpenLocalInFileManager: AnAction? = Actions.getActionByNameWithNullSupplier(Actions.OpenLocalInFileManagerAction)

    private val globalExplorer: AnAction? = Actions.getActionByNameWithNullSupplier(Actions.MakeAConnectionAction)
    private val globalDropdown: AnAction? = Actions.getActionByNameWithNullSupplier(Actions.RemoteMemoSelectorDropdownAction)
    private val globalReload: AnAction? = Actions.getActionByNameWithNullSupplier(Actions.ReloadRemoteAction)
    private val globalSuspend: AnAction? = Actions.getActionByNameWithNullSupplier(Actions.DisconnectAction)
    private val globalNewTerminal: AnAction? = Actions.getActionByNameWithNullSupplier(Actions.NewTerminalAction)
    private val globalLocalToggle: AnAction? = Actions.getActionByNameWithNullSupplier(Actions.ToggleVisibilityLocalListAction)

    // endregion

    // region UI组件

    private var panelWrapper = JPanel(BorderLayout())
    private val splitter: JBSplitter = OnePixelSplitter("xftp-main-window", .5f)

    private var localWrapper = JPanel(GridBagLayout())
    private var localPathWrapper = JPanel(GridBagLayout())
    val localPath = MemoComboBox<String>(LOCAL_HISTORY_PERSISTENCE_KEY)
    protected var localFileList: FileTable = FileTable()
    private var localFileListWrapper: JBScrollPane = JBScrollPane(localFileList)
    private var localActionGroup = DefaultActionGroup()
    private var localActionToolBar = ActionToolbarImpl(LOCAL_TOOL_BAR_PLACE, localActionGroup, false)

    // region 本地actions图标按钮

    val reloadLocalActionButton = SimpleMutableAction(globalReloadLocal) {
        reloadLocal()
    }
    val openLocalInFileManager = SimpleMutableAction(globalOpenLocalInFileManager) {
        getCurrentLocalPath().let { path ->
            try {
                Desktop.getDesktop().open(File(path))
            } catch (ioException: IOException) {
                ioException.printStackTrace()
                XFTPManager.message("Failed to open \"$path\"", MessageType.ERROR)
            }
        }
    }

    // endregion

    protected var remoteWrapper = JPanel(GridBagLayout())
    private var remotePathWrapper = JPanel(GridBagLayout())
    val remotePath = MemoComboBox<String>(REMOTE_HISTORY_PERSISTENCE_KEY)
    private var remoteActionGroup = DefaultActionGroup()
    private var remoteActionToolBar = ActionToolbarImpl(REMOTE_TOOL_BAR_PLACE, remoteActionGroup, false)
    var remoteFileList = FileTable()
        protected set
    private var remoteFileListWrapper = JBScrollPane(remoteFileList)

    // region 远程actions图标按钮

    // 建立连接
    val explore: MutableAction = SimpleMutableAction(globalExplorer) {
        if (!isConnected()) {
            connect {
                if (!XFTPManager.toolWindow.isVisible) XFTPManager.toolWindow.show()
            }
        }
    }
    // 显示combobox的下拉内容
    val dropdown: MutableAction = SimpleMutableAction(globalDropdown) {
        if (isConnected()) {
            remotePath.focusAndPopup()
        }
    }
    // 刷新
    val reload: MutableAction = SimpleMutableAction(globalReload) {
        if (isConnected()) {
            reloadRemote()
        }
    }
    // 断开连接
    val suspend: MutableAction = SimpleMutableAction(globalSuspend) {
        if (isConnected() && isChannelAlive()) {
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
    val newTerminal: MutableAction = SimpleMutableAction(globalNewTerminal) {
        if (isConnected()) {
            openInNewTerminal(remotePath.getMemoItem())
        }
    }
    // 隐藏本地浏览器
    val localToggle: MutableAction = SimpleMutableAction(globalLocalToggle) { e ->
        val to: Boolean = !splitter.firstComponent.isVisible
        splitter.firstComponent.isVisible = to
        e.presentation.icon = if (to) AllIcons.Diff.ApplyNotConflictsRight else AllIcons.Diff.ApplyNotConflictsLeft
    }

    // endregion

    // region 远程右键菜单

    protected val remoteFileListPopupMenu = JBPopupMenu()

    // 返回上一层
    val cdDotDot = JBMenuItem("cd ..")
    // 打开
    val cdOrCat = JBMenuItem("cd/cat")
    // 删除
    val rmRf = JBMenuItem(RM_RF_TEXT)
    // 克隆
    val duplicate = JBMenuItem(CP_TEXT)
    // 重命名
    val mv = JBMenuItem(MV_TEXT)
    // 新建文件
    val touch = JBMenuItem(TOUCH_TEXT)
    // 新建文件夹
    val mkdirp = JBMenuItem(MKDIR_P_TEXT)

    // endregion

    // endregion

    /**
     * 初始化UI样式
     */
    protected open fun buildUI() {
        this.buildLocalUI()
        this.bindLocalIU()
        this.buildRemoteUI()
        this.bindRemoteUI()

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

        localWrapper.border = null

        localActionGroup.add(reloadLocalActionButton)
        localActionGroup.add(openLocalInFileManager)
        localActionToolBar.targetComponent = panelWrapper
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

        remoteWrapper.border = null

        // 远程列表窗口组件
        remoteActionGroup.addAll(
            explore,
            dropdown,
            reload,
            suspend,
            newTerminal,
            Separator.create(),
            localToggle,
        )
        remoteActionToolBar.targetComponent = panelWrapper

        resetRemoteListContentMenuItemsText()
        remoteFileListPopupMenu.add(cdDotDot)
        remoteFileListPopupMenu.add(cdOrCat)
        remoteFileListPopupMenu.addSeparator()
        remoteFileListPopupMenu.add(rmRf)
        remoteFileListPopupMenu.addSeparator()
        remoteFileListPopupMenu.add(duplicate)
        remoteFileListPopupMenu.add(mv)
        remoteFileListPopupMenu.addSeparator()
        remoteFileListPopupMenu.add(touch)
        remoteFileListPopupMenu.add(mkdirp)
        remoteFileList.componentPopupMenu = remoteFileListPopupMenu
    }

    /**
     * 给本地UI组件表绑定事件
     */
    abstract fun bindLocalIU()

    /**
     * 给远程UI组件表绑定事件
     */
    abstract fun bindRemoteUI()

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
        explore.enabled = !enable
        dropdown.enabled = enable
        reload.enabled = enable
        suspend.enabled = enable
        newTerminal.enabled = enable
    }

    /**
     * 设置远程列表右键菜单的状态
     * @param enable 是否启用
     */
    protected fun setRemoteListContentMenuItems(enable: Boolean) {
        cdDotDot.isEnabled = enable
        cdOrCat.isEnabled = enable
        rmRf.isEnabled = enable
        duplicate.isEnabled = enable
        mv.isEnabled = enable
        touch.isEnabled = enable
        mkdirp.isEnabled = enable
    }

    /**
     * 重置远程列表右键菜单的text
     */
    protected fun resetRemoteListContentMenuItemsText() {
        cdOrCat.text = "$CD_TEXT ..."
        rmRf.text = "$RM_RF_TEXT ..."
        duplicate.text = "$CP_TEXT ... ..."
        mv.text = "$MV_TEXT ... ..."
        touch.text = TOUCH_TEXT
        mkdirp.text = MKDIR_P_TEXT
    }

    /**
     * 设置当前远程内容锁定
     */
    protected open fun lockRemoteUIs() {
        application.invokeLater {
            remoteFileListPopupMenu.isEnabled = false
            remoteWrapper.isEnabled = false
            remoteFileList.isEnabled = false
            remotePath.isEnabled = false
        }
    }

    /**
     * 设置当前远程内容锁定
     */
    protected open fun unlockRemoteUIs() {
        application.invokeLater {
            remoteFileListPopupMenu.isEnabled = true
            remoteWrapper.isEnabled = true
            remoteFileList.isEnabled = true
            remotePath.isEnabled = true
        }
    }

    /**
     * 执行application.executeOnPooledThread并锁定远程列表
     * @param callback 返回值将用于是否刷新远程列表
     * @param errConsumer 出错时将调用的方法
     */
    protected open fun executeOnPooledThreadWithRemoteUILocked(callback: Supplier<Boolean>, errConsumer: Consumer<Exception>? = null) {
        application.executeOnPooledThread {
            lockRemoteUIs()
            var requiredReload = true
            try {
                requiredReload = callback.get()
            } catch (err: Exception) {
                err.printStackTrace()
                errConsumer?.consume(err)
            }
            unlockRemoteUIs()
            if (requiredReload) reloadRemote()
        }
    }

    /**
     * 手动触发[JMenuItem]的事件
     * @param menuItem 触发事件的菜单项
     */
    open fun performAnJMenuItemAction(menuItem: JMenuItem) {
        menuItem.actionListeners.forEach { actionListener ->
            actionListener.actionPerformed(object : ActionEvent(menuItem, ACTION_PERFORMED, null) {})
        }
    }

}