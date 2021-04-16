package net.allape.bus;

import com.intellij.notification.*;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;

public final class Services {

    /**
     * message中用到的group
     */
    public static final String GROUP = "xftp";

    /**
     * 当前tool window
     */
    public static ToolWindow TOOL_WINDOW = null;

    /**
     * 消息提醒
     * @param message 提示的消息
     */
    public static void message (String message, MessageType type) {
        NotificationGroup notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup(GROUP);
        Notification notification = notificationGroup.createNotification(message, type);
        Notifications.Bus.notify(notification);
    }
}
