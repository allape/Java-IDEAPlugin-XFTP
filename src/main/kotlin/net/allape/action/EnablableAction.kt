package net.allape.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

abstract class EnablableAction(icon: Icon? = null) : DumbAwareAction(icon) {

    var enabled = true

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = enabled
    }
}