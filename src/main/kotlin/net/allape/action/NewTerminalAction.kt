package net.allape.action

import com.intellij.openapi.actionSystem.AnActionEvent
import icons.TerminalIcons
import net.allape.common.XFTPManager

class NewTerminalAction : EnablableAction(TerminalIcons.OpenTerminal_13x13) {
    override fun actionPerformed(e: AnActionEvent) {
        if (enabled) {
            XFTPManager.getCurrentSelectedWindow()?.apply {
                if (isConnected()) {
                    openInNewTerminal(remotePath.getMemoItem())
                }
            }
        }
    }
}