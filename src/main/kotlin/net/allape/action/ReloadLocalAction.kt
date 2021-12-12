package net.allape.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager

class ReloadLocalAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.reloadLocalActionButton?.actionPerformed(e)
    }

}