package net.allape.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import icons.TerminalIcons
import net.allape.common.XFTPManager

class NewTerminalAction : DumbAwareAction(TerminalIcons.OpenTerminal_13x13) {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.apply {
            this.newTerminal.actionPerformed(e)
        }
    }
}