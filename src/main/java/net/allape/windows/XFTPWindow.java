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
import net.allape.models.FileTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class XFTPWindow {

    static final protected String WINDOW_GROUP = "xftp";

    static final protected String USER_HOME = System.getProperty("user.home");

    static final protected long DOUBLE_CLICK_INTERVAL = 350;

    // 双击监听
    protected long clickWatcher = System.currentTimeMillis();

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
     * 排序文件
     * @param files 需要排序的文件
     */
    static protected List<FileModel> sortFiles (List<FileModel> files) {
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

        return sortedList;
    }

    /**
     * 将文件内容放入ListUI
     * @param ui 使用的UI
     * @param files 展示的文件列表
     */
    static protected void rerenderFileList (JList<FileModel> ui, List<FileModel> files) {
        // 清空列表后将现在的内容添加进去
        ui.clearSelection();
        if (!(ui.getModel() instanceof DefaultListModel)) {
            ui.setModel(new DefaultListModel<>());
        }
        ui.setListData(sortFiles(files).toArray(new FileModel[]{}));
    }

    /**
     * 将文件内容放入TableUI
     * @param ui 使用的UI
     * @param files 展示的文件列表
     */
    static protected void rerenderFileTable (JTable ui, List<FileModel> files) {
        if (!(ui.getModel() instanceof FileTableModel)) {
            ui.setModel(new FileTableModel());

            TableColumn typeColumn = ui.getColumnModel().getColumn(0);
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(JLabel.CENTER);
            typeColumn.setCellRenderer(centerRenderer);
            typeColumn.setMaxWidth(25);
        }
        FileTableModel model = ((FileTableModel) (ui.getModel()));
        model.resetData(sortFiles(files));
    }

}
