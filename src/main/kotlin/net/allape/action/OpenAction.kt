package net.allape.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager

class OpenAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.apply {
            if (localPath.isPopupVisible) {
                localPath.isPopupVisible = false
            } else if (remotePath.isPopupVisible) {
                remotePath.isPopupVisible = false
            } else {
                performAnJMenuItemAction(open)
            }
        }
    }
}