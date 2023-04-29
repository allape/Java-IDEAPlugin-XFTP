package net.allape.common

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.GotItTooltip
import net.allape.xftp.XFTP
import net.allape.xftp.XFTPPanel
import java.awt.Point
import javax.swing.JComponent


class XFTPManager {

    companion object {
        const val TOOL_WINDOW_ID = "XFTP"

        // 默认的窗口名称
        const val DEFAULT_NAME = "Explorer"

        /**
         * message中用到的group
         */
        private const val GROUP = "xftp"

        private val GOT_IT_ID = "${XFTPManager::javaClass.javaClass.canonicalName}_tooltips_id"

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
            val tooltips = GotItTooltip(GOT_IT_ID, message, getCurrentSelectedWindow())
            tooltips.showCondition =  { true }
            tooltips
                .withTimeout(3000)
                .withPosition(Balloon.Position.above)
                .show(component) { c, _ ->
                    Point(c.width / 10, 0)
                }
        }

        fun getCurrentSelectedWindow(): XFTP? {
            return getCurrentProjectToolWindow()?.let { toolWindow ->
                val selectedComponent = toolWindow.contentManager.selectedContent?.component as XFTPPanel?
                return selectedComponent?.xftp
            }
        }

        fun getCurrentProject(): Project? {
            return IdeFocusManager.getGlobalInstance().lastFocusedFrame?.project
        }

        fun getCurrentProjectToolWindow(): ToolWindow? {
            return getCurrentProject()?.let { project ->
                return ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)
            }
        }
    }

}