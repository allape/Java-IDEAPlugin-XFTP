package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager

class ToggleVisibilityLocalListAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.let { window ->
            val to: Boolean = !window.splitter.firstComponent.isVisible
            window.splitter.firstComponent.isVisible = to
            window.localToggle.setIcon(if (to) AllIcons.Diff.ApplyNotConflictsRight else AllIcons.Diff.ApplyNotConflictsLeft)
        }
    }
}