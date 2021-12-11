package net.allape.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

abstract class EnablableAction(
    private val toolbar: ActionToolbarImpl,
    text: String,
    description: String,
    private var icon: Icon,
) : DumbAwareAction(text, description, icon) {

    private var enabled = true

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun setIcon(icon: Icon?) {
        this.icon = icon!!
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = enabled
        e.presentation.icon = icon
    }
}