package net.allape.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

@Deprecated("NewFolderAction is deprecated")
class NewFolderAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
//        XFTPManager.getCurrentSelectedWindow()?.apply {
//            if (isRemoteListFocused()) performAnJMenuItemAction(mkdirp)
//        }
    }
}