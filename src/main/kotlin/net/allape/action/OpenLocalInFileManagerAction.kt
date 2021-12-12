package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.MessageType
import net.allape.common.XFTPManager
import java.awt.Desktop
import java.io.File
import java.io.IOException

class OpenLocalInFileManagerAction : AnAction(AllIcons.Actions.MenuOpen) {

    override fun actionPerformed(e: AnActionEvent) {
        XFTPManager.getCurrentSelectedWindow()?.apply {
            getCurrentLocalPath().let { path ->
                try {
                    Desktop.getDesktop().open(File(path))
                } catch (ioException: IOException) {
                    ioException.printStackTrace()
                    XFTPManager.message("Failed to open \"$path\"", MessageType.ERROR)
                }
            }
        }
    }

}