package net.allape.actions;

import com.google.gson.Gson;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.MessageType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;

public class MenuOpenAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        NotificationGroup notificationGroup = new NotificationGroup("testid", NotificationDisplayType.BALLOON, false);

        try {
            // 工具包
            Class util = Class.forName("com.jetbrains.plugins.remotesdk.RemoteSdkUtil");
            // 配置内容
            Class config = Class.forName("com.intellij.ssh.config.unified.SshConfig");
            Field[] configFields = config.getDeclaredFields();

            Project project = ProjectManager.getInstance().getDefaultProject();
            List<Object> sshConfigs = (List<Object>) util.getDeclaredMethod("getSshConfigList", Project.class).invoke(project);
            for (Object o : sshConfigs) {
                for (Field f : configFields) {
                    System.out.println(f.get(o));
                }
            }

            Gson gson = new Gson();
            Notification notification = notificationGroup.createNotification(gson.toJson(sshConfigs), MessageType.INFO);
            Notifications.Bus.notify(notification);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
