package net.allape.windows;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.DarculaColors;
import com.intellij.ui.JBColor;
import net.allape.dialogs.Confirm;
import net.allape.models.FileModel;
import net.allape.models.FileTableModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class XFTPExplorerWindow extends XFTPWindow {

    // region UI objects

    private JPanel panel;

    private JScrollPane localFs;
    private JList<FileModel> localFiles;
    private JTextField localFsPath;

    private JScrollPane remoteFs;
    private JTextField remoteFsPath;
    private JTable remoteFiles;

    // endregion

    private FileModel lastLocalModel = new FileModel(USER_HOME, "home sweet home", true);
    private FileModel currentLocalModel = lastLocalModel;
    private List<FileModel> selectedLocalModels = new ArrayList<>(0);
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
        this.setDefaultTheme(this.localFsPath);
        this.setDefaultTheme(this.localFiles);
        this.localFiles.setSelectionBackground(JBColor.namedColor(
                "Plugins.lightSelectionBackground",
                DarculaColors.BLUE
        ));
        this.localFiles.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        this.setDefaultTheme(this.remoteFs);
        this.remoteFs.setBorder(null);
        this.setDefaultTheme(this.remoteFsPath);
        this.setDefaultTheme(this.remoteFiles);
        this.remoteFiles.setSelectionBackground(JBColor.namedColor(
                "Plugins.lightSelectionBackground",
                DarculaColors.BLUE
        ));
        this.remoteFiles.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.remoteFiles.setAutoCreateRowSorter(false);
    }

    /**
     * 初始化UI行为
     */
    @SuppressWarnings("unused")
    private void initUIAction () {
        this.localFiles.setCellRenderer(new XFTPExplorerWindowListCellRenderer());
        // 设置当前选中的内容
        this.localFiles.addListSelectionListener(e -> {
            final XFTPExplorerWindow self = XFTPExplorerWindow.this;
            self.selectedLocalModels = self.localFiles.getSelectedValuesList();
            self.lastLocalModel = self.currentLocalModel;
            self.currentLocalModel = self.localFiles.getSelectedValue();
        });
        // 监听双击, 双击后打开文件或文件夹
        this.localFiles.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final XFTPExplorerWindow self = XFTPExplorerWindow.this;
                long now = System.currentTimeMillis();
                if (self.lastLocalModel == self.currentLocalModel && now - self.localFileClickWatcher < DOUBLE_CLICK_INTERVAL) {
                    self.loadLocal(self.currentLocalModel.getPath());
                }
                self.localFileClickWatcher = now;
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

        this.remoteFiles.setModel(new FileTableModel());
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
                File[] files = file.listFiles();
                if (files == null) {
                    new Confirm(
                        new Confirm.ConfirmOptions()
                            .title("It is a N/A folder")
                            .content("This folder is not available, do you want to open it in system file manager?")
                            .onOk(e -> {
                                try {
                                    Desktop.getDesktop().open(file);
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                    message("Failed to open file in system file manager", MessageType.INFO);
                                }
                            })
                    );
                    return;
                }

                path = file.getAbsolutePath();
                this.localFsPath.setText(path);
                this.remoteFsPath.setText(path); // FIXME 测试

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
                rerenderFileTable(this.remoteFiles, fileModels); // FIXME 测试
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
