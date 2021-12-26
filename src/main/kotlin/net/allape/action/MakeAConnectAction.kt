package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import net.allape.common.XFTPManager
import net.allape.xftp.XFTP

class MakeAConnectAction: DumbAwareAction(AllIcons.Webreferences.Server) {

    override fun actionPerformed(e: AnActionEvent) {
        val window = XFTPManager.getCurrentSelectedWindow()
        if (window == null) {
            XFTP.createWindowWithAnActionEvent(e, false) {
                makeNewConnection(it, e)
            }
        } else {
            makeNewConnection(window, e)
        }
    }

    private fun makeNewConnection(window: XFTP, e: AnActionEvent) = window.apply {
        if (!isConnected()) {
            window.explore.actionPerformed(e)
        }
    }
}