package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import net.allape.common.XFTPManager

class OpenLocalInFileManagerAction : DumbAwareAction(AllIcons.Actions.MenuOpen) {

    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.apply {
            this.openLocalInFileManager.actionPerformed(e)
        }
    }

}