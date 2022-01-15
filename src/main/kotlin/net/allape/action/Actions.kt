package net.allape.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.keymap.KeymapUtil
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.function.Supplier
import javax.swing.KeyStroke

class Actions {

    companion object {

        // 判断是否为macOS系统, 只适配使用了meta键和没用使用到meta键的系统
        val MetaHeavy: Boolean = System.getProperty("os.name")?.lowercase()?.startsWith("mac os x") ?: false

        const val NewWindowAction = "XFTP.NewWindow"

        const val ReloadLocalAction = "XFTP.ReloadLocal"

        const val OpenLocalInFileManagerAction = "XFTP.OpenLocalInFileManager"

        const val MakeAConnectionAction = "XFTP.MakeAConnection"

        const val RemoteMemoSelectorDropdownAction = "XFTP.RemoteMemoSelectorDropdown"

        const val ReloadRemoteAction = "XFTP.ReloadRemote"

        const val DisconnectAction = "XFTP.Disconnect"

        const val NewTerminalAction = "XFTP.NewTerminal"

        const val ToggleVisibilityLocalListAction = "XFTP.ToggleVisibilityLocalList"

        // deprecated

        @Deprecated("XFTP.Open is no long a global action")
        const val OpenAction = "XFTP.Open"
        @Deprecated("XFTP.GoUpper is no long a global action")
        const val GoUpperAction = "XFTP.GoUpper"
        @Deprecated("XFTP.Delete is no long a global action")
        const val DeleteAction = "XFTP.Delete"
        @Deprecated("XFTP.Duplicate is no long a global action")
        const val DuplicateAction = "XFTP.Duplicate"
        @Deprecated("XFTP.Rename is no long a global action")
        const val RenameAction = "XFTP.Rename"
        @Deprecated("XFTP.NewFile is no long a global action")
        const val NewFileAction = "XFTP.NewFile"
        @Deprecated("XFTP.NewFolder is no long a global action")
        const val NewFolderAction = "XFTP.NewFolder"

        val OpenActionKeymap = SimpleKeymap(
            default = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
        )
        val GoUpperActionKeymap = SimpleKeymap(
            default = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK),
            macOS = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK or InputEvent.META_DOWN_MASK),
        )
        val DeleteActionKeymap = SimpleKeymap(
            default = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            macOS = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.META_DOWN_MASK),
        )
        val DuplicateActionKeymap = SimpleKeymap(
            default = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK),
            macOS = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.META_DOWN_MASK),
        )
        val RenameActionKeymap = SimpleKeymap(
            default = KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK),
        )
        val NewFileActionKeymap = SimpleKeymap(
            default = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK),
            macOS = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.META_DOWN_MASK),
        )
        val NewFolderActionKeymap = SimpleKeymap(
            default = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
            macOS = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.META_DOWN_MASK),
        )

        /**
         * 根据名称获取AnAction
         * @param id AnAction名称
         * @param defaultProvider 获取到的action为null时的替代方案
         */
        fun <T> getActionByNameWithNullSupplier(id: String, defaultProvider: Supplier<T>? = null): T? where T: AnAction{
            @Suppress("UNCHECKED_CAST")
            return (ActionManager.getInstance().getAction(id) ?: defaultProvider?.get()) as T?
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

data class SimpleKeymap(
    val default: KeyStroke,
    val macOS: KeyStroke? = null,
) {
    fun toKeystroke(): KeyStroke = (if (Actions.MetaHeavy) macOS else null) ?: default
}
