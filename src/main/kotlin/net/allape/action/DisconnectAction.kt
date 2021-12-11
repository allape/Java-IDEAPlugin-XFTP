package net.allape.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager

class DisconnectAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.let { window ->
            window.sftpChannel?.isConnected?.let {
                if (it) {
                    window.disconnect()
                }
            }
        }
    }
}