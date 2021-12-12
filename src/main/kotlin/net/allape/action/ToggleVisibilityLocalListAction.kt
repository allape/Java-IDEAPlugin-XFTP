package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager

class ToggleVisibilityLocalListAction: EnablableAction(AllIcons.Diff.ApplyNotConflictsRight) {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.let { window ->
            val to: Boolean = !window.splitter.firstComponent.isVisible
            window.splitter.firstComponent.isVisible = to
            e.presentation.icon = if (to) AllIcons.Diff.ApplyNotConflictsRight else AllIcons.Diff.ApplyNotConflictsLeft
        }
    }
}