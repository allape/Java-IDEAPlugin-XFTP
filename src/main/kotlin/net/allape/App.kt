package net.allape

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import net.allape.action.NewWindowAction
import net.allape.common.XFTPManager
import net.allape.xftp.XFTP

class App: ToolWindowFactory, DumbAware {

    companion object {

        fun createTheToolWindowContent(project: Project, toolWindow: ToolWindow) {
            // window实例
            val window = XFTP(project, toolWindow)

            //获取内容工厂的实例
            val contentFactory = ContentFactory.SERVICE.getInstance()

            //获取用于toolWindow显示的内容
            val content = contentFactory.createContent(window.getUI(), XFTPManager.DEFAULT_NAME, false)
            content.isCloseable = true
            content.putUserData(XFTP.XFTP_KEY, window)

            //给toolWindow设置内容
            val contentManager = toolWindow.contentManager
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)
            window.bindContext(content)

            // 放入缓存
            XFTPManager.windows[content] = window
        }
    }

    private val logger = Logger.getInstance(App::class.java)

    override fun init(toolWindow: ToolWindow) {
        XFTPManager.toolWindow = toolWindow
        toolWindow.addContentManagerListener(object : ContentManagerListener {
            override fun contentRemoved(event: ContentManagerEvent) {
                val window = XFTPManager.windows[event.content]
                if (window == null) {
                    logger.warn("closed an un-cached window: $event")
                } else {
                    window.dispose()
                    XFTPManager.windows.remove(event.content)
                }
                if (toolWindow.contentManager.contents.isEmpty()) {
                    toolWindow.hide()
                }
            }
        })
        if (toolWindow is ToolWindowEx) {
            toolWindow.setTabActions(NewWindowAction())
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 默认创建一个XFTP窗口
        createTheToolWindowContent(project, toolWindow)
    }
}