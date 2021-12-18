package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager

class RemoteMemoSelectorDropdownAction: EnablableAction(AllIcons.Actions.MoveDown) {
    override fun actionPerformed(e: AnActionEvent) {
        if (enabled) {
            XFTPManager.getCurrentSelectedWindow()?.apply {
                if (isConnected()) {
                    remotePath.focusAndPopup()
                }
            }
        }
    }
}
