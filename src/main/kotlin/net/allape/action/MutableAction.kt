package net.allape.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Consumer
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

class SimpleMutableAction(
    private val from: AnAction? = null,
    private val consumer: Consumer<AnActionEvent>,
) : MutableAction(
    from?.templatePresentation?.text,
    from?.templatePresentation?.description,
    from?.templatePresentation?.icon,
    from?.shortcutSet,
) {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        templatePresentation.text = from?.templatePresentation?.text
        templatePresentation.description = from?.templatePresentation?.description
        templatePresentation.icon = from?.templatePresentation?.icon
        from?.shortcutSet?.let {
            shortcutSet = it
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (enabled) {
            consumer.accept(e)
        }
    }

}
