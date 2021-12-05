package net.allape.component

import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.util.Consumer
import net.allape.xftp.ExplorerBaseWindow
import java.awt.event.InputEvent
import java.util.*

/**
 * @sample com.intellij.ide.actions.CreateFileAction
 */
class FileNameTextFieldDialog(private val project: Project) {

    fun openDialog (isDirectory: Boolean, value: String? = null, consumer: Consumer<String>) {
        val validator = FileNameValidator(isDirectory, value)
        if (Experiments.getInstance().isFeatureEnabled("show.create.new.element.in.popup")) {
            createLightWeightPopup(validator, value, consumer).showCenteredInCurrentWindow(project)
        } else {
            Messages.showInputDialog(
                this.project, "Create something new",
                "New ${if (isDirectory) "Folder" else "File"}", null, value, validator
            )
        }
    }

    private fun createLightWeightPopup(validator: FileNameValidator, value: String? = null, consumer: Consumer<String>): JBPopup {
        val contentPanel = NewItemSimplePopupPanel()
        val nameField = contentPanel.textField
        if (value != null) nameField.text = value
        return NewItemPopupUtil.createNewItemPopup("New ${validator.objectName.replaceFirstChar { it.uppercaseChar() }}", contentPanel, nameField).also { popup ->
            contentPanel.applyAction = Consumer { event: InputEvent? ->
                val name = nameField.text
                if (validator.checkInput(name) && validator.canClose(name)) {
                    popup.closeOk(event)
                    consumer.consume(name)
                } else {
                    contentPanel.setError(validator.getErrorText(name))
                }
            }
        }
    }

}

class FileNameValidator(
    private val isDirectory: Boolean,
    private val value: String? = null,
) : InputValidatorEx {

    val objectName: String = if (isDirectory) "folder" else "file"

    private var errorText: String? = null

    override fun checkInput(inputString: String?): Boolean {
        if (inputString == null || inputString.isEmpty()) {
            errorText = "$objectName name can NOT be empty"
            return false
        }
        val parsedName = inputString.replace("\\", ExplorerBaseWindow.FILE_SEP)
        if (!isDirectory && inputString.endsWith(ExplorerBaseWindow.FILE_SEP)) {
            errorText = "$objectName name can NOT end withs ${ExplorerBaseWindow.FILE_SEP}"
            return false
        }
        if (value == inputString) {
            errorText = "$value already exists"
            return false
        }
        val tokenizer = StringTokenizer(
            parsedName,
            ExplorerBaseWindow.FILE_SEP,
        )
        while (tokenizer.hasMoreTokens()) {
            val token = tokenizer.nextToken()
            if ((token == "." || token == "..")) {
                errorText = "invalid $objectName name: $token"
                return false
            }
        }
        errorText = null
        return true
    }

    override fun canClose(inputString: String?): Boolean = checkInput(inputString)

    override fun getErrorText(inputString: String?): String? = errorText

}
