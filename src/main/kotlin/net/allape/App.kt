package net.allape

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory
import net.allape.action.NewWindowAction
import net.allape.common.XFTPManager
import net.allape.xftp.XFTP

class App: ToolWindowFactory, DumbAware {

    companion object {

        fun createTheToolWindowContent(project: Project, toolWindow: ToolWindow): XFTP {
            // window实例
            val window = XFTP(project, toolWindow)

            //获取内容工厂的实例
            val contentFactory = ContentFactory.getInstance()

            //获取用于toolWindow显示的内容
            val content = contentFactory.createContent(window.getUI(), XFTPManager.DEFAULT_NAME, false)
            content.isCloseable = true
            content.putUserData(XFTP.XFTP_KEY, window)

            //给toolWindow设置内容
            val contentManager = toolWindow.contentManager
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)

            window.bindContext(content)

            return window
        }
    }

    override fun init(toolWindow: ToolWindow) {
        if (toolWindow is ToolWindowEx) {
            toolWindow.setTabActions(NewWindowAction())
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 默认创建一个XFTP窗口
        createTheToolWindowContent(project, toolWindow)
    }
}