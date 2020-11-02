package net.allape.windows;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import net.allape.models.FileModel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class XFTPWindow {

    static final protected String WINDOW_GROUP = "xftp";

    static final protected String USER_HOME = System.getProperty("user.home");

    static final protected long DOUBLE_CLICK_INTERVAL = 200;

    final protected Project project;
    final protected ToolWindow toolWindow;

    public XFTPWindow (Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
    }

    /**
     * 设置为IDE自带的样式
     * @param component 操作的控件
     */
    protected void setDefaultTheme (JComponent component) {
        component.setBackground(JBColor.background());
        component.setForeground(JBColor.foreground());
    }

    /**
     * 消息提醒
     * @param message 提示的消息
     */
    static protected void message (String message, MessageType type) {
        NotificationGroup notificationGroup = new NotificationGroup(WINDOW_GROUP, NotificationDisplayType.BALLOON, false);
        Notification notification = notificationGroup.createNotification(message, type);
        Notifications.Bus.notify(notification);
    }

    /**
     * 将文件内容放入ListUI
     * @param ui 使用的UI
     * @param files 展示的文件列表
     */
    static protected void rerenderFileList(JList<FileModel> ui, List<FileModel> files) {
        // 将文件根据文件夹->文件、文件名称排序
        files.sort(Comparator.comparing(FileModel::getName));
        List<FileModel> filesOnly = new ArrayList<>(files.size());
        List<FileModel> foldersOnly = new ArrayList<>(files.size());
        for (FileModel model : files) {
            if (model.getFolder()) {
                foldersOnly.add(model);
            } else {
                filesOnly.add(model);
            }
        }
        List<FileModel> sortedList = new ArrayList<>(files.size());
        sortedList.addAll(foldersOnly);
        sortedList.addAll(filesOnly);

        // 清空列表后将现在的内容添加进去
        ui.clearSelection();
        ui.setModel(new DefaultListModel<>());
        ui.setListData(sortedList.toArray(new FileModel[]{}));
    }

}
