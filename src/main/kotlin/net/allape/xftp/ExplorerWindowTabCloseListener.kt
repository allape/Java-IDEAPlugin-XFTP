package net.allape.xftp

import com.intellij.execution.ui.BaseContentCloseListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.ui.content.Content
import net.allape.common.Windows

class ExplorerWindowTabCloseListener(
    private val content: Content,
    private val project: Project,
    disposable: Disposable,
): BaseContentCloseListener(content, project, disposable) {

    override fun disposeContent(content: Content) {}

    override fun closeQuery(content: Content, projectClosing: Boolean): Boolean {
        if (projectClosing) return true
        val window = Windows.windows[content]
        return if (window?.getSftpClient() != null) {
            MessageDialogBuilder.yesNo("A server is connected", "Do you really want to close this tab?")
                .asWarning()
                .yesText("Close")
                .ask(project)
        } else true
    }

    override fun canClose(project: Project): Boolean =
        project === this.project && closeQuery(content, true)
}