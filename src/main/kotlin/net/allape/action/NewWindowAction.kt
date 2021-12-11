package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import net.allape.App
import net.allape.common.XFTPManager

class NewWindowAction : DumbAwareAction(
    "New Window",
    "Open a new XFTP window",
    AllIcons.General.Add,
) {

    init {
        shortcutSet = KeymapUtil.getActiveKeymapShortcuts(Actions.NewWindowAction)
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            XFTPManager.toolWindow.let { toolWindow ->
                toolWindow.show { App.createTheToolWindowContent(project, toolWindow) }
            }
        }
    }
}