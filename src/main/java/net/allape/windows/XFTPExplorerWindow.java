package net.allape.windows;

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

public class XFTPExplorerWindow extends XFTPWindow {

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
        this.initUIStyle(project, toolWindow);
        this.initUIAction(project, toolWindow);

        this.loadLocal(this.localPath);
    }

    /**
     * 初始化UI样式
     */
    private void initUIStyle (Project project, ToolWindow toolWindow) {
        this.setDefaultTheme(this.panel);
        this.setDefaultTheme(this.localFs);
        this.setDefaultTheme(this.localFiles);

        this.setDefaultTheme(this.remoteFs);
    }

    /**
     * 初始化UI行为
     */
    private void initUIAction (Project project, ToolWindow toolWindow) {
        this.localFiles.setCellRenderer(new XFTPExplorerWindowListCellRenderer());
        this.localFiles.addListSelectionListener(e -> {
            XFTPExplorerWindow.this.currentLocalModel = XFTPExplorerWindow.this.localFiles.getSelectedValue();
        });
        this.localFiles.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (System.currentTimeMillis() - XFTPExplorerWindow.this.localFileClickWatcher < DOUBLE_CLICK_INTERVAL) {
                    XFTPExplorerWindow.this.loadLocal(XFTPExplorerWindow.this.currentLocalModel.getPath());
                    System.out.println("打开" + XFTPExplorerWindow.this.currentLocalModel.getPath());
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

            rerenderFileList(this.localFiles, fileModels);
        } catch (Exception e) {
            e.printStackTrace();
            message(e.getMessage(), MessageType.WARNING);
        }
    }

    /**
     * 获取JPanel
     * @return JPanel
     */
    public JPanel getPanel () {
        return this.panel;
    }

}
