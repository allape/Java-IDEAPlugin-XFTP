package net.allape.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindow
import net.allape.App
import net.allape.common.Services

class MenuOpenAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            val toolWindow: ToolWindow = Services.toolWindow
            toolWindow.show { App.createTheToolWindowContent(project, toolWindow) }
        }
    }
}