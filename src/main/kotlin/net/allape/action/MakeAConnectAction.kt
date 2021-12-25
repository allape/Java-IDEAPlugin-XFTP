package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager
import net.allape.xftp.XFTP

class MakeAConnectAction: EnablableAction(AllIcons.Webreferences.Server) {

    override fun actionPerformed(e: AnActionEvent) {
        if (enabled) {
            val window = XFTPManager.getCurrentSelectedWindow()
            if (window == null) {
                XFTP.createWindowWithAnActionEvent(e, false) {
                    makeNewConnection(it)
                }
            } else {
                makeNewConnection(window)
            }
        }
    }

    private fun makeNewConnection(window: XFTP) = window.apply {
        if (!isConnected()) {
            connect {
                if (!XFTPManager.toolWindow.isVisible) XFTPManager.toolWindow.show()
            }
        }
    }
}