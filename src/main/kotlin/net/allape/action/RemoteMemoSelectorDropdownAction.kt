package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import net.allape.common.XFTPManager

class RemoteMemoSelectorDropdownAction: DumbAwareAction(AllIcons.Actions.MoveDown) {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.apply {
            this.dropdown.actionPerformed(e)
        }
    }
}
