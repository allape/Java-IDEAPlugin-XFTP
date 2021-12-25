package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager

class MakeAConnectAction: EnablableAction(AllIcons.Webreferences.Server) {

    override fun actionPerformed(e: AnActionEvent) {
        if (enabled) {
            XFTPManager.getCurrentSelectedWindow()?.apply {
                if (!isConnected()) {
                    connect {
                        if (XFTPManager.toolWindow.isVisible) XFTPManager.toolWindow.show()
                    }
                }
            }
        }
    }
}