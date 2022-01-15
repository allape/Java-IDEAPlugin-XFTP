package net.allape.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

@Deprecated("DuplicateAction is deprecated")
class DuplicateAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
//        XFTPManager.getCurrentSelectedWindow()?.apply {
//            if (isRemoteListFocused()) performAnJMenuItemAction(duplicate)
//        }
    }
}