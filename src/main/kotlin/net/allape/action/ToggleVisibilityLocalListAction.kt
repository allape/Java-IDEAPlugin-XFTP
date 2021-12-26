package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import net.allape.common.XFTPManager

class ToggleVisibilityLocalListAction: DumbAwareAction(AllIcons.Diff.ApplyNotConflictsRight) {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.apply {
            this.localToggle.actionPerformed(e)
        }
    }
}