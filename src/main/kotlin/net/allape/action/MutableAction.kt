package net.allape.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

abstract class MutableAction(
    text: String? = null,
    desc: String? = null,
    icon: Icon? = null,
    ss: ShortcutSet? = null,
) : DumbAwareAction(
    text,
    desc,
    icon,
) {

    var enabled = true

    init {
        ss?.let {
            this.shortcutSet = it
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = enabled
    }
}