package net.allape

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import net.allape.common.Services
import net.allape.common.Windows
import net.allape.xftp.Explorer
import java.util.function.Supplier

class App: ToolWindowFactory, DumbAware {

    companion object {

        fun createTheToolWindowContent(project: Project, toolWindow: ToolWindow) {
            // window实例
            val window = Explorer(project, toolWindow)

            //获取内容工厂的实例
            val contentFactory = ContentFactory.SERVICE.getInstance()

            //获取用于toolWindow显示的内容
            val content = contentFactory.createContent(window.getUI(), Windows.WINDOW_DEFAULT_NAME, false)
            content.isCloseable = true

            //给toolWindow设置内容
            val contentManager = toolWindow.contentManager
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)
            window.bindContext(content)

            // 放入缓存
            Windows.windows[content] = window
        }
    }

    private val logger = Logger.getInstance(App::class.java)

    override fun init(toolWindow: ToolWindow) {
        Services.toolWindow = toolWindow
        toolWindow.addContentManagerListener(object : ContentManagerListener {
            override fun contentRemoved(event: ContentManagerEvent) {
                val window = Windows.windows.get(event.content)
                if (window == null) {
                    logger.warn("closed an un-cached window: $event")
                } else {
                    window.dispose()
                    Windows.windows.remove(event.content)
                }
                if (toolWindow.contentManager.contents.isEmpty()) {
                    toolWindow.hide()
                }
            }
        })
        if (toolWindow is ToolWindowEx) {
            toolWindow.setTabActions(object : DumbAwareAction(
                Supplier { "New Explorer" },
                Supplier { "Open a new explorer" }, AllIcons.General.Add
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    if (e.project != null) {
                        this@App.createToolWindowContent(e.project!!, toolWindow)
                    } else {
                        Services.message("Explorer requires a project!", MessageType.INFO)
                    }
                }
            })
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 默认创建一个XFTP窗口
        createTheToolWindowContent(project, toolWindow)
    }
}