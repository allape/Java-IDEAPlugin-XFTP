package net.allape.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager
import java.awt.event.ActionEvent


class NewFileAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.let { window ->
            window.touch.action.
            actionPerformed(object : ActionEvent(window.touch, ACTION_PERFORMED, null) {})
        }
    }
}