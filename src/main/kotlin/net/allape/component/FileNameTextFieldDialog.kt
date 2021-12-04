package net.allape.component

import com.intellij.ide.IdeBundle
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.lang.LangBundle
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.ssh.channels.SftpChannel
import com.intellij.util.Consumer
import net.allape.xftp.ExplorerBaseWindow
import java.awt.event.InputEvent
import java.util.*

/**
 * @sample com.intellij.ide.actions.CreateFileAction
 */
class FileNameTextFieldDialog(private val project: Project) {

    fun openDialog (channel: SftpChannel, consumer: Consumer<String>) {
        val validator = FileNameValidator(channel)
        if (Experiments.getInstance().isFeatureEnabled("show.create.new.element.in.popup")) {
            createLightWeightPopup(validator, consumer).showCenteredInCurrentWindow(project)
        } else {
            Messages.showInputDialog(
                this.project, IdeBundle.message("prompt.enter.new.file.name"),
                IdeBundle.message("title.new.file"), null, null, validator
            )
        }
    }

    private fun createLightWeightPopup(validator: InputValidator, consumer: Consumer<String>): JBPopup {
        val contentPanel = NewItemSimplePopupPanel()
        val nameField = contentPanel.textField
        return NewItemPopupUtil.createNewItemPopup(IdeBundle.message("title.new.file"), contentPanel, nameField).also { popup ->
            contentPanel.applyAction = Consumer { event: InputEvent? ->
                val name = nameField.text
                if (validator.checkInput(name) && validator.canClose(name)) {
                    popup.closeOk(event)
                    consumer.consume(name)
                } else {
                    val errorMessage =
                        if (validator is InputValidatorEx)
                            validator.getErrorText(name)
                        else
                            LangBundle.message("incorrect.name")
                    contentPanel.setError(errorMessage)
                }
            }
        }
    }

}

class FileNameValidator(private val channel: SftpChannel) : InputValidatorEx {

    private var errorText: String? = null

    override fun checkInput(inputString: String?): Boolean {
        val tokenizer = StringTokenizer(
            inputString?.replace("\\", ExplorerBaseWindow.FILE_SEP),
            ExplorerBaseWindow.FILE_SEP,
        )
        while (tokenizer.hasMoreTokens()) {
            val token = tokenizer.nextToken()
            if ((token == "." || token == "..") && !tokenizer.hasMoreTokens()) {
                errorText = "invalid file name: $token"
                return false
            }
        }
        errorText = null
        return true
    }

    override fun canClose(inputString: String?): Boolean = checkInput(inputString)

    override fun getErrorText(inputString: String?): String? = errorText

}
