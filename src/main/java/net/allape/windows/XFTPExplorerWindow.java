package net.allape.windows;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.remote.RemoteConnectionType;
import com.intellij.ssh.ConnectionBuilder;
import com.intellij.ssh.RemoteCredentialsUtil;
import com.intellij.ssh.RemoteFileObject;
import com.intellij.ssh.channels.SftpChannel;
import com.intellij.ui.DarculaColors;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.ContentManagerEvent;
import com.jetbrains.plugins.remotesdk.console.RemoteDataProducer;
import net.allape.dialogs.Confirm;
import net.allape.models.FileModel;
import net.allape.models.FileTableModel;
import net.allape.models.XftpSshConfig;

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
    private JButton exploreButton;
    private JButton disconnectButton;
    private JTable remoteFiles;

    // endregion

    // 上一次选择的本地文件
    private FileModel lastLocalModel = new FileModel(USER_HOME, "home sweet home", true);
    // 当前选中的本地文件
    private FileModel currentLocalModel = lastLocalModel;
    // 当前选中的所有文件
    private List<FileModel> selectedLocalModels = new ArrayList<>(0);

    // 当前开启的channel
    private SftpChannel sftpChannel = null;

    public XFTPExplorerWindow(Project project, ToolWindow toolWindow) {
        super(project, toolWindow);
        this.initUIStyle();
        this.initUIAction();

        this.loadLocal(this.currentLocalModel.getPath());
    }

    @Override
    public void onClosed(ContentManagerEvent e) {
        super.onClosed(e);


        System.out.println("啊, 我被关了");
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
        this.setDefaultTheme(this.remoteFiles);
        this.setDefaultTheme(this.exploreButton);
        this.setDefaultTheme(this.disconnectButton);
        this.disconnectButton.setBackground(DarculaColors.RED);
        this.disconnectButton.setForeground(JBColor.white);
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
        this.exploreButton.addActionListener(e -> {
            this.connectSftp();
        });
        this.disconnectButton.addActionListener(e -> {
            this.disconnect(true);
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

                List<FileModel> fileModels = new ArrayList<>(file.length() == 0 ? 1 : (file.length() > Integer.MAX_VALUE ?
                        Integer.MAX_VALUE :
                        Integer.parseInt(String.valueOf(file.length()))
                ));

                // 添加返回上一级目录
                fileModels.add(this.getLastFolder(path));

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
     * 使用当前选择的配置进行连接
     */
    public void connectSftp () {
        // SftpChannel connection = ConnectionBuilder.openSftpChannel$default(
        //   RemoteCredentialsUtil.connectionBuilder$default(
        //     this.credentials,
        //     (Project)null,
        //     (ProgressIndicator)null,
        //     3,
        //     (Object)null
        //   ),
        //   0,
        //   1,
        //   (Object)null
        // );

        // (new RemoteDataProducer())
        //   .withActionEvent(event)
        //   .withShowProjectLevelServers(true)
        //   .produceRemoteData(
        //      optionsProvider.getConnectionType(),
        //      optionsProvider.getConnectionId(),
        //      optionsProvider.getAdditionalData(),
        //      (data) -> {}
        //    )
        // com.jetbrains.plugins.remotesdk.console.RemoteDataProducer

        this.triggerConnecting();
        try {
            this.disconnect(false);
            (new RemoteDataProducer())
                    .withProject(this.project)
                    .produceRemoteData(
                            RemoteConnectionType.SSH_CONFIG,
                            null,
                            "",
                            data -> {
                                ConnectionBuilder connectionBuilder = RemoteCredentialsUtil.connectionBuilder(data, this.project);
                                this.sftpChannel = connectionBuilder.openSftpChannel();
                                this.triggerConnected();

                                this.loadRemote(this.sftpChannel.getHome());
                            }
                    );
        } catch (Exception e) {
            e.printStackTrace();
            this.triggerDisconnected();
            message(e.getMessage(), MessageType.ERROR);
        }
    }

    /**
     * 获取远程文件目录
     * @param path 默认地址, 为null时自动使用sftp默认文件夹
     */
    public void loadRemote (String path) {
        if (this.sftpChannel == null) {
            message("Please connect to server first!", MessageType.INFO);
        } else if (!this.sftpChannel.isConnected()) {
            message("SFTP lost connection, retrying...", MessageType.ERROR);
            this.connectSftp();
        }

        RemoteFileObject file = this.sftpChannel.file(path);
        if (!file.exists()) {
            message(path + " does not exist!", MessageType.WARNING);
        } if (!file.isDir()) {
            // TODO 打开文件并监听更改, 更改后自动上传
        } else {
            this.remoteFsPath.setText(path);

            List<RemoteFileObject> files = file.list();
            List<FileModel> fileModels = new ArrayList<>(files.size());

            // 添加返回上一级目录
            fileModels.add(this.getLastFolder(path));

            for (RemoteFileObject f : files) {
                fileModels.add(new FileModel(f.path(), f.name(), f.isDir()));
            }

            rerenderFileTable(this.remoteFiles, fileModels);
        }
    }

    /**
     * 断开当前连接
     */
    public void disconnect (boolean triggerEvent) {
        if (this.sftpChannel != null && this.sftpChannel.isConnected()) {
            this.sftpChannel.disconnect();
            this.sftpChannel = null;

            // 清空列表
            ((FileTableModel) (this.remoteFiles.getModel())).resetData(new ArrayList<>());

            if (triggerEvent) {
                this.triggerDisconnected();
            }
        }
    }

    /**
     * 设置当前状态为连接中
     */
    public void triggerConnecting () {
        this.exploreButton.setEnabled(false);
    }

    /**
     * 设置当前状态为已连接
     */
    public void triggerConnected () {
        this.exploreButton.setVisible(false);
        this.disconnectButton.setVisible(true);
    }

    /**
     * 设置当前状态为未连接
     */
    public void triggerDisconnected () {
        this.exploreButton.setVisible(true);
        this.disconnectButton.setVisible(false);
    }

    /**
     * 获取JPanel
     * @return JPanel
     */
    public JPanel getPanel () {
        return this.panel;
    }

    /**
     * 获取上一级目录
     * @param path 当前目录
     */
    private FileModel getLastFolder (String path) {
        // 添加返回上一级目录
        int lastIndexOfSep = path.lastIndexOf(File.separator);
        String parentFolder = lastIndexOfSep == -1 ? "" : path.substring(0, lastIndexOfSep);
        return new FileModel(parentFolder.isEmpty() ? File.separator : parentFolder , "..", true);
    }

}
