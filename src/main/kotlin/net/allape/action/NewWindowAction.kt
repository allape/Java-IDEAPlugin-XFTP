package net.allape.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.App
import net.allape.common.XFTPManager

class NewWindowAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            XFTPManager.toolWindow.let { toolWindow ->
                toolWindow.show { App.createTheToolWindowContent(project, toolWindow) }
            }
        }
    }
}