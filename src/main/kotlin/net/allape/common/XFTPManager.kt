package net.allape.common

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.Notifications
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import net.allape.xftp.XFTP

class XFTPManager {

    companion object {

        lateinit var toolWindow: ToolWindow

        // 默认的窗口名称
        const val DEFAULT_NAME = "Explorer"

        // 当前打开的窗口
        val windows: HashMap<Content, XFTP> = HashMap(10)

        /**
         * message中用到的group
         */
        private const val GROUP = "xftp"

        /**
         * 消息提醒
         * @param message 提示的消息
         */
        fun message(message: String, type: MessageType = MessageType.WARNING) {
            val notificationGroup =
                NotificationGroupManager.getInstance().getNotificationGroup(GROUP)
            val notification = notificationGroup.createNotification(
                message,
                type
            )
            Notifications.Bus.notify(notification)
        }

        /**
         * 获取当前选中的窗口, 可能为null
         */
        fun getCurrentSelectedWindow(): XFTP? {
            return toolWindow.contentManager.selectedContent?.let { content ->
                windows[content]
            }
        }

    }

}