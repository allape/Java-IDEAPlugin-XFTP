package net.allape.xftp

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ssh.RemoteFileObject
import com.intellij.ui.JBSplitter
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Consumer
import net.allape.action.EnablableAction
import net.allape.component.FileTable
import net.allape.component.MemoComboBox
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.datatransfer.DataFlavor
import java.util.function.Supplier
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 纯UI组件
 */
abstract class ExplorerWindowUI(
    project: Project,
    toolWindow: ToolWindow,
) : ExplorerBaseWindow(project, toolWindow) {

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

        const val RM_RF_TEXT = "rm -Rf"
        const val MV_TEXT = "mv"
        const val TOUCH_TEXT = "touch"
        const val CP_TEXT = "cp"
        const val MKDIR_P_TEXT = "mkdir -p"
    }

    // region UI组件

    protected var panelWrapper = JPanel(BorderLayout())
    val splitter: JBSplitter = OnePixelSplitter("xftp-main-window", .5f)

    protected var localWrapper = JPanel(GridBagLayout())
    protected var localPathWrapper = JPanel(GridBagLayout())
    val localPath = MemoComboBox<String>(LOCAL_HISTORY_PERSISTENCE_KEY)
    protected var localFileList: FileTable = FileTable()
    protected var localFileListWrapper: JBScrollPane = JBScrollPane(localFileList)
    protected var localActionGroup = DefaultActionGroup()
    protected var localActionToolBar = ActionToolbarImpl(LOCAL_TOOL_BAR_PLACE, localActionGroup, false)
    protected var remoteWrapper = JPanel(GridBagLayout())

    protected var remotePathWrapper = JPanel(GridBagLayout())
    val remotePath = MemoComboBox<String>(REMOTE_HISTORY_PERSISTENCE_KEY)
    protected var remoteActionGroup = DefaultActionGroup()
    protected var remoteActionToolBar = ActionToolbarImpl(REMOTE_TOOL_BAR_PLACE, remoteActionGroup, false)
    protected var remoteFileList = FileTable()
    protected var remoteFileListWrapper = JBScrollPane(remoteFileList)

    // region 远程actions图标按钮

    // 建立连接
    protected lateinit var explore: EnablableAction
    // 显示combobox的下拉内容
    protected lateinit var dropdown: EnablableAction
    // 刷新
    protected lateinit var reload: EnablableAction
    // 断开连接
    protected lateinit var suspend: EnablableAction
    // 命令行打开
    protected lateinit var newTerminal: EnablableAction
    // 隐藏本地浏览器
    lateinit var localToggle: EnablableAction
        protected set

    // endregion

    // region 远程右键菜单

    protected val remoteFileListPopupMenu = JBPopupMenu()

    // 删除
    protected val rmRf = JBMenuItem(RM_RF_TEXT)
    // 克隆
    protected val duplicate = JBMenuItem(CP_TEXT)
    // 重命名
    protected val mv = JBMenuItem(MV_TEXT)
    // 新建文件
    protected val touch = JBMenuItem(TOUCH_TEXT)
    // 新建文件夹
    protected val mkdirp = JBMenuItem(MKDIR_P_TEXT)

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

        resetRemoteListContentMenuItemsText()
        remoteFileListPopupMenu.add(rmRf)
        remoteFileListPopupMenu.add(duplicate)
        remoteFileListPopupMenu.add(mv)
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
        explore.setEnabled(!enable)
        dropdown.setEnabled(enable)
        reload.setEnabled(enable)
        suspend.setEnabled(enable)
        newTerminal.setEnabled(enable)
    }

    /**
     * 设置远程列表右键菜单的状态
     * @param enable 是否启用
     */
    protected fun setRemoteListContentMenuItems(enable: Boolean) {
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

}