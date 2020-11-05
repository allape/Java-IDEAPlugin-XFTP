package net.allape.windows;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
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
import org.jetbrains.annotations.NotNull;
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

    // 上一次选择文件
    private FileModel lastFile = null;

    // 当前选中的本地文件
    private FileModel currentLocalFile = null;
    // 当前选中的所有文件
    private List<FileModel> selectedLocalFiles = new ArrayList<>(0);

    // 当前开启的channel
    private SftpChannel sftpChannel = null;
    // 当前选中的远程文件
    private FileModel currentRemoteFile = null;
    // 当前选中的所有文件
    private List<FileModel> selectedRemoteFiles = new ArrayList<>(0);

    public XFTPExplorerWindow(Project project, ToolWindow toolWindow) {
        super(project, toolWindow);
        this.initUIStyle();
        this.initUIAction();

        this.loadLocal(USER_HOME);
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
        this.remoteFiles.setSelectionBackground(JBColor.namedColor(
                "Plugins.lightSelectionBackground",
                DarculaColors.BLUE
        ));
        this.remoteFiles.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.remoteFiles.setAutoCreateRowSorter(false);
        this.remoteFiles.setBorder(null);
    }

    /**
     * 初始化UI行为
     */
    @SuppressWarnings("unused")
    private void initUIAction () {
        this.localFiles.setCellRenderer(new XFTPExplorerWindowListCellRenderer());
        // 设置当前选中的内容
        this.localFiles.addListSelectionListener(e -> {
            this.selectedLocalFiles = this.localFiles.getSelectedValuesList();
            this.lastFile = this.currentLocalFile;
            this.currentLocalFile = this.localFiles.getSelectedValue();
        });
        // 监听双击, 双击后打开文件或文件夹
        this.localFiles.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final XFTPExplorerWindow self = XFTPExplorerWindow.this;
                long now = System.currentTimeMillis();
                if (self.lastFile == self.currentLocalFile && now - self.clickWatcher < DOUBLE_CLICK_INTERVAL) {
                    self.loadLocal(self.currentLocalFile.getPath());
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
        this.exploreButton.addActionListener(e -> this.connectSftp());
        this.disconnectButton.addActionListener(e -> this.disconnect(true));
        this.remoteFiles.getSelectionModel().addListSelectionListener(e -> {
            List<FileModel> allRemoteFiles = ((FileTableModel) this.remoteFiles.getModel()).getData();

            int[] selectedRows = this.remoteFiles.getSelectedRows();
            this.selectedRemoteFiles = new ArrayList<>(selectedRows.length);
            for (int i : selectedRows) {
                this.selectedRemoteFiles.add(allRemoteFiles.get(i));
            }

            int currentSelectRow = this.remoteFiles.getSelectedRow();
            if (currentSelectRow != -1) {
                this.lastFile = this.currentRemoteFile;
                this.currentRemoteFile = allRemoteFiles.get(currentSelectRow);
            }
        });
        this.remoteFiles.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final XFTPExplorerWindow self = XFTPExplorerWindow.this;
                long now = System.currentTimeMillis();
                if (self.lastFile == self.currentRemoteFile && now - self.clickWatcher < DOUBLE_CLICK_INTERVAL) {
                    self.loadRemote(self.currentRemoteFile.getPath());
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
                if (file.length() > EDITABLE_FILE_SIZE) {
                    DialogWrapper dialog = new Confirm(new Confirm.ConfirmOptions()
                            .title("This file is too large for text editor")
                            .content("Do you still want to edit it?"));
                    if (dialog.showAndGet()) {
                        this.openFileInEditor(file, null);
                    }
                } else {
                    this.openFileInEditor(file, null);
                }
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
        try {
            (new RemoteDataProducer())
                    .withProject(this.project)
                    .produceRemoteData(
                            RemoteConnectionType.SSH_CONFIG,
                            null,
                            "",
                            data -> {
                                this.triggerConnecting();
                                this.disconnect(false);

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
            // 如果文件小于2M, 则自动下载到缓存目录并进行监听
            if (file.size() > EDITABLE_FILE_SIZE) {
                DialogWrapper dialog = new Confirm(new Confirm.ConfirmOptions()
                        .title("This file is too large for text editor")
                        .content("Do you still want to download and edit it?"));
                if (dialog.showAndGet()) {
                    this.downloadFileAndEdit(file);
                }
            } else {
                this.downloadFileAndEdit(file);
            }
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
        }

        // 清空列表
        if (this.remoteFiles.getModel() instanceof FileTableModel) {
            ((FileTableModel) this.remoteFiles.getModel()).resetData(new ArrayList<>());
        }

        if (triggerEvent) {
            this.triggerDisconnected();
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
        this.exploreButton.setEnabled(true);
        this.exploreButton.setVisible(true);
        this.disconnectButton.setVisible(false);
        this.remoteFsPath.setText("");
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

    /**
     * 上传文件, 并通知bus
     * @param localFile 本地文件
     * @param remoteFile 线上文件
     */
    private void uploadFile (File localFile, RemoteFileObject remoteFile) {
        // TODO 上传文件
        System.out.println("上传" + localFile.getAbsolutePath() + " -> " + remoteFile.path());
    }

    /**
     * 下载文件并编辑
     * @param file 远程文件信息
     */
    private void downloadFileAndEdit (RemoteFileObject file) {
        if (file.isDir()) {
            throw new IllegalArgumentException("Can not edit a folder!");
        } else if (this.sftpChannel == null || !this.sftpChannel.isConnected()) {
            message("Please start a sftp session first!", MessageType.INFO);
            return;
        }

        try {
            this.panel.setEnabled(false);
            final File localFile = File.createTempFile("jb-ide-xftp", file.name());
            this.sftpChannel.downloadFileOrDir(file.path(), localFile.getAbsolutePath());

            this.openFileInEditor(localFile, new BulkFileListener() {
                @Override
                public void before(@NotNull List<? extends VFileEvent> events) {
                    for (VFileEvent e : events) {
                        if (e.isFromSave()) {
                            VirtualFile virtualFile = e.getFile();
                            if (virtualFile != null) {
                                if (localFile.getAbsolutePath().equals(virtualFile.getPath())) {
                                    // 上传文件
                                    XFTPExplorerWindow.this.uploadFile(localFile, file);
                                }
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            message("error occurred while downloading file [" + file.path() + "], " + e.getMessage(), MessageType.ERROR);
        }
        this.panel.setEnabled(true);
    }

    /**
     * 打开文件
     * @param file 文件
     */
    private void openFileInEditor (@NotNull File file, @Nullable BulkFileListener listener) {
        Editor editor = FileEditorManager.getInstance(this.project).openTextEditor(
                new OpenFileDescriptor(
                        this.project,
                        Objects.requireNonNull(LocalFileSystem.getInstance().findFileByIoFile(file)),
                        0
                ),
                true
        );

        // 设置文件监听
        if (listener != null) {
            this.project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, listener);
        }
    }

}
