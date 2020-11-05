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
import net.allape.models.XftpSshConfig;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ItemEvent;
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
    private JTextField localFsPath;
    private JList<FileModel> localFiles;

    private JScrollPane remoteFs;
    private JTextField remoteFsPath;
    private JPanel remoteFsWrapper;
    private JPanel sshConfigCBWrapper;
    // com.jetbrains.plugins.remotesdk.ui.RemoteSdkBySshConfigForm
    // com.intellij.ssh.ui.unified.SshConfigComboBox
    private JComboBox<XftpSshConfig> sshConfigComboBox;
    private JButton exploreButton;
    private JTable remoteFiles;

    // endregion

    // 上一次选择的本地文件
    private FileModel lastLocalModel = new FileModel(USER_HOME, "home sweet home", true);
    // 当前选中的本地文件
    private FileModel currentLocalModel = lastLocalModel;
    // 当前选中的所有文件
    private List<FileModel> selectedLocalModels = new ArrayList<>(0);

    // 当前选中的ssh配置
    private XftpSshConfig xftpSshConfig = null;

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
        this.setDefaultTheme(this.remoteFsWrapper);
        this.setDefaultTheme(this.remoteFsPath);
        this.setDefaultTheme(this.sshConfigCBWrapper);
        this.setDefaultTheme(this.sshConfigComboBox);
        this.setDefaultTheme(this.remoteFiles);
        this.setDefaultTheme(this.exploreButton);
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
                if (self.lastLocalModel == self.currentLocalModel && now - self.clickWatcher < DOUBLE_CLICK_INTERVAL) {
                    self.loadLocal(self.currentLocalModel.getPath());
                }
                self.clickWatcher = now;
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

        // 弹出的时候获取ssh配置
        this.sshConfigComboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                final XFTPExplorerWindow self = XFTPExplorerWindow.this;
                List<XftpSshConfig> configs = XftpSshConfig.getConfigs();
                self.sshConfigComboBox.removeAllItems();
                configs.forEach(config -> self.sshConfigComboBox.addItem(config));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        this.sshConfigComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final XFTPExplorerWindow self = XFTPExplorerWindow.this;
                XftpSshConfig config = (XftpSshConfig) self.sshConfigComboBox.getSelectedItem();
                System.out.println(config);
            }
        });
        this.exploreButton.addActionListener(e -> {
            final XFTPExplorerWindow self = XFTPExplorerWindow.this;
            if (self.xftpSshConfig == null) {
                DialogWrapper dialog = new Confirm(new Confirm.ConfirmOptions()
                        .title("No Config is selected")
                        .content("Please choose a config to connect")
                );
                dialog.show();
            } else {
                this.loadRemote(null);
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
                File[] files = file.listFiles();
                if (files == null) {
                    DialogWrapper dialog = new Confirm(
                        new Confirm.ConfirmOptions()
                            .title("It is an unavailable folder!")
                            .content("This folder is not available, do you want to open it in system file manager?")
                    );
                    if (dialog.showAndGet()) {
                        try {
                            Desktop.getDesktop().open(file);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                            message("Failed to open file in system file manager", MessageType.INFO);
                        }
                    }
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
     * 获取远程文件目录
     * @param path 默认地址, 为null时自动使用sftp默认文件夹
     */
    public void loadRemote (@Nullable String path) {

    }

    /**
     * 获取JPanel
     * @return JPanel
     */
    public JPanel getPanel () {
        return this.panel;
    }

}
