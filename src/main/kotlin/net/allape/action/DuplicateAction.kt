package net.allape.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager

class DuplicateAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.let { window ->
            window.performAnJMenuItemAction(window.duplicate)
        }
    }
}