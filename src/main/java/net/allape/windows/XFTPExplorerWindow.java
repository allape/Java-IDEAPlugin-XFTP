package net.allape.windows;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindow;
import net.allape.models.FileModel;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XFTPExplorerWindow {

    static final private String WINDOW_GROUP = "xftp";

    static final private String USER_HOME = System.getProperty("user.home");

    static final private long DOUBLE_CLICK_INTERVAL = 100;

    // region UI objects

    private JPanel panel;

    private JScrollPane localFs;
    private JList<FileModel> localFiles;

    private JScrollPane remoteFs;

    // endregion

    private String localPath = USER_HOME;
    private FileModel currentLocalModel = null;
    private long localFileClickWatcher = System.currentTimeMillis();

    public XFTPExplorerWindow(Project project, ToolWindow toolWindow) {
        this.localFiles.addListSelectionListener(e -> {
            XFTPExplorerWindow.this.currentLocalModel = (FileModel) e.getSource();
        });
        this.localFiles.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (System.currentTimeMillis() - XFTPExplorerWindow.this.localFileClickWatcher < DOUBLE_CLICK_INTERVAL) {
                    XFTPExplorerWindow.this.loadLocal(XFTPExplorerWindow.this.currentLocalModel.getPath());
                }
                XFTPExplorerWindow.this.localFileClickWatcher = System.currentTimeMillis();
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        this.loadLocal(this.localPath);
    }

    /**
     * 获取本地文件目录
     * @param path 路径
     */
    public void loadLocal (String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                message(path + " does not exist!", MessageType.WARNING);
            } else if (!file.isDirectory()) {
                message(path + " id not a folder!", MessageType.WARNING);
            }

            File[] files = file.listFiles();
            List<FileModel> fileModels = new ArrayList<>(file.length() > Integer.MAX_VALUE ?
                    Integer.MAX_VALUE :
                    Integer.parseInt(String.valueOf(file.length()))
            );
            for (File currentFile : files) {
                FileModel model = new FileModel();
                model.setName(currentFile.getName());
                model.setPath(currentFile.getAbsolutePath());
                model.setFolder(currentFile.isDirectory());

                fileModels.add(model);
            }

            this.reloadList(this.localFiles, fileModels);
        } catch (Exception e) {
            e.printStackTrace();
            message(e.getMessage(), MessageType.WARNING);
        }
    }

    /**
     * 将文件内容放入ListUI
     * @param ui 使用的UI
     * @param files 展示的文件列表
     */
    private void reloadList (JList<FileModel> ui, List<FileModel> files) {
        ui.clearSelection();
        ui.setListData(files.toArray(new FileModel[]{}));
    }

    /**
     * 获取JPanel
     * @return JPanel
     */
    public JPanel getPanel () {
        return this.panel;
    }

    /**
     * 消息提醒
     * @param message 提示的消息
     */
    public static void message (String message, MessageType type) {
        NotificationGroup notificationGroup = new NotificationGroup(WINDOW_GROUP, NotificationDisplayType.BALLOON, false);
        Notification notification = notificationGroup.createNotification(message, type);
        Notifications.Bus.notify(notification);
    }

}
