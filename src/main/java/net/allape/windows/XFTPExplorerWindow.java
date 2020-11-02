package net.allape.windows;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.DarculaColors;
import com.intellij.ui.JBColor;
import net.allape.models.FileModel;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class XFTPExplorerWindow extends XFTPWindow {

    // region UI objects

    private JPanel panel;

    private JScrollPane localFs;
    private JList<FileModel> localFiles;

    private JScrollPane remoteFs;

    // endregion

    private FileModel currentLocalModel = new FileModel(USER_HOME, "root", true);
    private long localFileClickWatcher = System.currentTimeMillis();

    public XFTPExplorerWindow(Project project, ToolWindow toolWindow) {
        super(project, toolWindow);
        this.initUIStyle();
        this.initUIAction();

        this.loadLocal(this.currentLocalModel.getPath());
    }

    /**
     * 初始化UI样式
     */
    @SuppressWarnings("unused")
    private void initUIStyle () {
        this.setDefaultTheme(this.panel);
        this.setDefaultTheme(this.localFs);
        this.localFs.setBorder(null);
        this.setDefaultTheme(this.localFiles);
        this.localFiles.setSelectionBackground(JBColor.namedColor("Plugins.lightSelectionBackground", DarculaColors.BLUE));

        this.setDefaultTheme(this.remoteFs);
        this.remoteFs.setBorder(null);
    }

    /**
     * 初始化UI行为
     */
    @SuppressWarnings("unused")
    private void initUIAction () {
        this.localFiles.setCellRenderer(new XFTPExplorerWindowListCellRenderer());
        this.localFiles.addListSelectionListener(e -> XFTPExplorerWindow.this.currentLocalModel = XFTPExplorerWindow.this.localFiles.getSelectedValue());
        this.localFiles.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                long now = System.currentTimeMillis();
                if (now - XFTPExplorerWindow.this.localFileClickWatcher < DOUBLE_CLICK_INTERVAL) {
                    XFTPExplorerWindow.this.loadLocal(XFTPExplorerWindow.this.currentLocalModel.getPath());
                }
                XFTPExplorerWindow.this.localFileClickWatcher = now;
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
                FileEditorManager.getInstance(this.project).openTextEditor(
                        new OpenFileDescriptor(
                                this.project,
                                Objects.requireNonNull(LocalFileSystem.getInstance().findFileByIoFile(file)),
                                0
                        ),
                        true
                );
//                message(file.getAbsolutePath() + " id not a folder!", MessageType.WARNING);
            } else {
                path = file.getAbsolutePath();

                File[] files = file.listFiles();
                List<FileModel> fileModels = new ArrayList<>(file.length() == 0 ? 1 : (file.length() > Integer.MAX_VALUE ?
                        Integer.MAX_VALUE :
                        Integer.parseInt(String.valueOf(file.length()))
                ));

                // 添加返回上一级目录
                int lastIndexOfSep = path.lastIndexOf(File.separator);
                String parentFolder = lastIndexOfSep == -1 ? "" : path.substring(0, lastIndexOfSep);
                fileModels.add(new FileModel(parentFolder.isEmpty() ? File.separator : parentFolder , "..", true));

                for (File currentFile : files) {
                    FileModel model = new FileModel();
                    model.setName(currentFile.getName());
                    model.setPath(currentFile.getAbsolutePath());
                    model.setFolder(currentFile.isDirectory());

                    fileModels.add(model);
                }

                rerenderFileList(this.localFiles, fileModels);
            }
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
