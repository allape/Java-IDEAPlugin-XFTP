package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import net.allape.common.XFTPManager

class ReloadLocalAction : DumbAwareAction(AllIcons.Actions.Refresh) {

    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.apply {
            this.reloadLocalActionButton.actionPerformed(e)
        }
    }

}