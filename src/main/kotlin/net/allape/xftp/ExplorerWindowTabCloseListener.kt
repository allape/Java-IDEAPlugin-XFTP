package net.allape.xftp

import com.intellij.execution.ui.BaseContentCloseListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.ui.content.Content

class ExplorerWindowTabCloseListener(
    private val project: Project,
    private val content: Content,
    disposable: Disposable,
): BaseContentCloseListener(content, project, disposable) {

    override fun disposeContent(content: Content) {
        try {
            (content.component as XFTPPanel).xftp.dispose()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun closeQuery(content: Content, projectClosing: Boolean): Boolean {
        if (projectClosing) return true
        val window = (content.component as XFTPPanel).xftp
        return if (window.sftpClient != null) {
            MessageDialogBuilder.yesNo("A server is connected", "Do you really want to close this tab?")
                .asWarning()
                .yesText("Close")
                .ask(project)
        } else true
    }

    override fun canClose(project: Project): Boolean =
        project === this.project && closeQuery(content, true)
}