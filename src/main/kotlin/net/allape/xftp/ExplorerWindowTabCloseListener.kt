package net.allape.xftp

import com.intellij.execution.ui.BaseContentCloseListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.ui.content.Content
import net.allape.common.XFTPManager

class ExplorerWindowTabCloseListener(
    private val content: Content,
    disposable: Disposable,
): BaseContentCloseListener(content, XFTPManager.getCurrentProject(), disposable) {

    override fun disposeContent(content: Content) {
        try {
            XFTPManager.windows[content]?.dispose()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun closeQuery(content: Content, projectClosing: Boolean): Boolean {
        if (projectClosing) return true
        val window = XFTPManager.windows[content]
        return if (window?.sftpClient != null) {
            MessageDialogBuilder.yesNo("A server is connected", "Do you really want to close this tab?")
                .asWarning()
                .yesText("Close")
                .ask(XFTPManager.getCurrentProject())
        } else true
    }

    override fun canClose(project: Project): Boolean =
        project === XFTPManager.getCurrentProject() && closeQuery(content, true)
}