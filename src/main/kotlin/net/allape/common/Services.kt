package net.allape.common

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.Notifications
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindow

class Services {

    companion object {

        lateinit var toolWindow: ToolWindow

        /**
         * message中用到的group
         */
        const val GROUP = "xftp"

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

    }

}