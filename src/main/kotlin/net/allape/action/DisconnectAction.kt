package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.MessageDialogBuilder
import net.allape.common.XFTPManager

class DisconnectAction: EnablableAction(AllIcons.Actions.Suspend) {
    override fun actionPerformed(e: AnActionEvent) {
        if (enabled) {
            XFTPManager.getCurrentSelectedWindow()?.apply {
                if (isConnected() && isChannelAlive()) {
                    if (MessageDialogBuilder.yesNo("Disconnecting", "Do you really want to close this session?")
                            .asWarning()
                            .yesText("Disconnect")
                            .ask(project)
                    ) {
                        disconnect()
                    }
                }
            }
        }
    }
}