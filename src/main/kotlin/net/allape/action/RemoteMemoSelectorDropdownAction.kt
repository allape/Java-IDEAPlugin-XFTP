package net.allape.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import net.allape.common.XFTPManager

class RemoteMemoSelectorDropdownAction: EnablableAction(AllIcons.Actions.MoveDown) {
    override fun actionPerformed(e: AnActionEvent) {
        if (enabled) {
            XFTPManager.getCurrentSelectedWindow()?.apply {
                if (isConnected()) {
                    // FIXME 通过快捷键打开的下拉, 在没有获取焦点的情况下会出现键盘无法控制选择项的问题
                    remotePath.isPopupVisible = !remotePath.isPopupVisible
                }
            }
        }
    }
}