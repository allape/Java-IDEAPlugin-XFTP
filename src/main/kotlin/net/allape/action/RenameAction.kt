package net.allape.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import net.allape.common.XFTPManager

class RenameAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.let { window ->
            window.performAnJMenuItemAction(window.mv)
        }
    }
}