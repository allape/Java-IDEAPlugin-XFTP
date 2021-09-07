package net.allape

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import net.allape.common.Services

class App: ToolWindowFactory, DumbAware {

    private val logger = Logger.getInstance(App::class.java)

    override fun init(toolWindow: ToolWindow) {
        Services.toolWindow = toolWindow
        super.init(toolWindow)
    }
}