package net.allape.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager

class ReloadRemoteAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.let { window ->
            if (window.sftpChannel != null && window.sftpChannel!!.isConnected) window.reloadRemote()
        }
    }

}