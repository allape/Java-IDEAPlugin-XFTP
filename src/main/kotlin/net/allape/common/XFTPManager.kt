package net.allape.common

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.Notifications
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.GotItTooltip
import com.intellij.ui.content.Content
import net.allape.xftp.XFTP
import java.awt.Point
import javax.swing.JComponent

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

        private val GOTIT_ID = "${XFTPManager::javaClass.javaClass.canonicalName}_tooltips_id"

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

        fun gotIt(component: JComponent, message: String) {
//            JBPopupFactory.getInstance().createComponentPopupBuilder(component, component).setTitle("啊哈").setAdText(message)
//            GotItMessage.createMessage("A", message).setShowCallout(false).show(RelativePoint(component, Point(0,0)), Balloon.Position.above)
            val tooltips = GotItTooltip(GOTIT_ID, message, ProjectManager.getInstance().defaultProject)
            tooltips.showCondition =  { true }
            tooltips
                .withTimeout(3000)
                .withPosition(Balloon.Position.above)
                .show(component) { c, _ ->
                    Point(c.width / 10, 0)
                }
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