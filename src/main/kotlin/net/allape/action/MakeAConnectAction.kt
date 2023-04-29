package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import net.allape.common.XFTPManager
import net.allape.xftp.XFTP

class MakeAConnectAction: DumbAwareAction(
    AllIcons.Webreferences.Server,
) {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        val window = XFTPManager.getCurrentSelectedWindow()
        if (window != null && window.isConnected()) {
            e.presentation.text = "Focus On Remote List"
            e.presentation.description = "Focus on remote file list of current XFTP explorer"
        } else {
            e.presentation.text = "Make A Connection"
            e.presentation.description = "Make a new connection to a server if there is no connection established"
        }
    }

    /**
     * @see [com.jetbrains.plugins.remotesdk.console.RunSshConsoleAction.actionPerformed]
     */
    override fun actionPerformed(e: AnActionEvent) {
        e.getData(CommonDataKeys.PROJECT)?.let { project ->
            val window = XFTPManager.getCurrentSelectedWindow()
            if (window == null) {

                XFTP.createWindowWithAnActionEvent(project, false) {
                    makeNewConnection(it, e)
                }
            } else {
                makeNewConnection(window, e)
            }
        }
    }

    private fun makeNewConnection(window: XFTP, e: AnActionEvent) = window.apply {
        if (!isConnected()) {
            window.explore.actionPerformed(e)
        } else {
            window.remoteFileList.requestFocusInWindow()
        }
    }
}