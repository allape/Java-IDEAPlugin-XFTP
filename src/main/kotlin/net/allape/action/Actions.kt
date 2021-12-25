package net.allape.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.keymap.KeymapUtil
import java.awt.event.KeyEvent
import java.util.function.Supplier
import javax.swing.KeyStroke

class Actions {

    companion object {

        const val NewWindowAction = "XFTP.NewWindow"

        const val ReloadLocalAction = "XFTP.ReloadLocal"

        const val OpenLocalInFileManagerAction = "XFTP.OpenLocalInFileManager"

        const val MakeAConnectionAction = "XFTP.MakeAConnection"

        const val RemoteMemoSelectorDropdownAction = "XFTP.RemoteMemoSelectorDropdown"

        const val ReloadRemoteAction = "XFTP.ReloadRemote"

        const val DisconnectAction = "XFTP.Disconnect"

        const val NewTerminalAction = "XFTP.NewTerminal"

        const val ToggleVisibilityLocalListAction = "XFTP.ToggleVisibilityLocalList"

        const val OpenAction = "XFTP.Open"

        const val GoUpperAction = "XFTP.GoUpper"

        const val DeleteAction = "XFTP.Delete"

        const val DuplicateAction = "XFTP.Duplicate"

        const val RenameAction = "XFTP.Rename"

        const val NewFileAction = "XFTP.NewFile"

        const val NewFolderAction = "XFTP.NewFolder"

        /**
         * 根据名称获取AnAction
         * @param id AnAction名称
         * @param defaultProvider 获取到的action为null时的替代方案
         */
        fun <T> getActionByNameWithNullSupplier(id: String, defaultProvider: Supplier<T>): T where T: AnAction{
            @Suppress("UNCHECKED_CAST")
            return (ActionManager.getInstance().getAction(id) ?: defaultProvider.get()) as T
        }

        /**
         * 获取action的第一组快捷键
         */
        fun getActionFirstKeyStroke(actionName: String): KeyStroke? {
            return KeymapUtil.getKeyStroke(KeymapUtil.getActiveKeymapShortcuts(actionName))
        }

        /**
         * 将快捷键转换为可读的内容
         */
        fun readableKeyStroke(keyStroke: KeyStroke?): String? {
            if (keyStroke == null) return null
            return (if (keyStroke.modifiers > 0) "${KeyEvent.getModifiersExText(keyStroke.modifiers)}+" else "") +
                    KeyEvent.getKeyText(keyStroke.keyCode)
        }

    }

}