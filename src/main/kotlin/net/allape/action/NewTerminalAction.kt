package net.allape.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import net.allape.common.XFTPManager
import org.jetbrains.plugins.terminal.TerminalIcons

class NewTerminalAction : DumbAwareAction(TerminalIcons.OpenTerminal_13x13) {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.apply {
            this.newTerminal.actionPerformed(e)
        }
    }
}