package net.allape.bus;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindow;
import net.allape.models.Transfer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
     * 上传线程池
     */
    public static final ExecutorService UPLOAD_POOL = new ThreadPoolExecutor(
            1, Integer.MAX_VALUE,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            (Runnable r, ThreadPoolExecutor executor) -> {
                Transfer transfer = Data.TRANSFERRING.remove(r);
                if (transfer != null) {
                    transfer.setResult(Transfer.Result.FAIL);
                    transfer.setException("Transferring queue is at capacity!");
                }
            }
    );

    /**
     * 消息提醒
     * @param message 提示的消息
     */
    public static void message (String message, MessageType type) {
        NotificationGroup notificationGroup = new NotificationGroup(GROUP, NotificationDisplayType.BALLOON, false);
        Notification notification = notificationGroup.createNotification(message, type);
        Notifications.Bus.notify(notification);
    }
}
