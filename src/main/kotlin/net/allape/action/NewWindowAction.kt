package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import net.allape.xftp.XFTP

class NewWindowAction : DumbAwareAction(
    "New XFTP Explorer",
    "Open a new XFTP explorer",
    AllIcons.General.Add,
) {

    init {
        shortcutSet = KeymapUtil.getActiveKeymapShortcuts(Actions.NewWindowAction)
    }

    override fun actionPerformed(e: AnActionEvent) {
        XFTP.createWindowWithAnActionEvent(e)
    }
}
