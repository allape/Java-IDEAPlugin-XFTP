package net.allape.windows.explorer;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
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
import com.intellij.ssh.impl.sshj.channels.SshjSftpChannel;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.util.ReflectionUtil;
import com.jetbrains.plugins.remotesdk.console.RemoteDataProducer;
import net.allape.bus.Data;
import net.allape.bus.Services;
import net.allape.dialogs.Confirm;
import net.allape.models.FileModel;
import net.allape.models.FileTableModel;
import net.allape.models.Transfer;
import net.allape.utils.Maps;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPFileTransfer;
import net.schmizz.sshj.xfer.TransferListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class XFTPExplorerWindow extends XFTPExplorerUI {

    // 上一次选择文件
    private FileModel lastFile = null;

    // 当前本地路径
    private String currentLocalPath = null;
    // 当前选中的本地文件
    private FileModel currentLocalFile = null;
    // 当前选中的所有文件 TODO 拖拽上传
    @SuppressWarnings("unused")
    private List<FileModel> selectedLocalFiles = new ArrayList<>(COLLECTION_SIZE);

    // 当前开启的channel
    private SftpChannel sftpChannel = null;
    // 当前channel中的sftp client
    private SFTPClient sftpClient = null;

    // 当前远程路径
    private String currentRemotePath = null;
    // 当前选中的远程文件
    private FileModel currentRemoteFile = null;
    // 当前选中的所有文件 TODO 拖拽下载
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private List<FileModel> selectedRemoteFiles = new ArrayList<>(COLLECTION_SIZE);

    // 修改中的远程文件, 用于文件修改后自动上传 key: remote file, value: local file
    private Map<RemoteFileObject, String> remoteEditingFiles = new HashMap<>(COLLECTION_SIZE);

    public XFTPExplorerWindow(Project project, ToolWindow toolWindow) {
        super(project, toolWindow);

        this.loadLocal(null);

        final XFTPExplorerWindow self = XFTPExplorerWindow.this;
        // 初始化文件监听
        if (this.project != null) {
            this.project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
                @Override
                public void after(@NotNull List<? extends VFileEvent> events) {
                    for (VFileEvent e : events) {
                        if (e.isFromSave()) {
                            VirtualFile virtualFile = e.getFile();
                            if (virtualFile != null) {
                                String localFile = virtualFile.getPath();
                                RemoteFileObject remoteFile = Maps.getFirstKeyByValue(self.remoteEditingFiles, localFile);
                                if (remoteFile != null) {
                                    // 上传文件
                                    self.uploadFile(localFile, remoteFile);
                                }
                            }
                        }
                    }
                }
            });
        } else {
            Services.message("Failed to initial file change listener", MessageType.ERROR);
        }
    }

    @Override
    public void onClosed(ContentManagerEvent e) {
        super.onClosed(e);

        // 关闭连接
        this.disconnect(true);
    }

    /**
     * 初始化UI行为
     */
    protected void initUI () {
        super.initUI();

        final XFTPExplorerWindow self = XFTPExplorerWindow.this;

        this.localPath.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                // 回车
                if (e.getKeyChar() == '\n') {
                    self.loadLocal(self.localPath.getText());
                }
            }
        });
        // 设置当前选中的内容
        this.localFileList.addListSelectionListener(e -> {
            this.selectedLocalFiles = this.localFileList.getSelectedValuesList();
            this.lastFile = this.currentLocalFile;
            this.currentLocalFile = this.localFileList.getSelectedValue();
        });
        // 监听双击, 双击后打开文件或文件夹
        this.localFileList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
        this.remotePath.setEnabled(false);
        this.remotePath.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    self.loadRemote(self.remotePath.getText());
                }
            }
        });
        this.exploreButton.addActionListener(e -> this.connectSftp());
        this.disconnectButton.setVisible(false);
        this.disconnectButton.addActionListener(e -> {
            DialogWrapper dialog = new Confirm(new Confirm.Options()
                    .title("Disconnecting")
                    .content("Do you really want to close this session?")
            );
            if (dialog.showAndGet()) {
                this.disconnect(true);
            }
        });
        this.remoteFileList.getSelectionModel().addListSelectionListener(e -> {
            List<FileModel> allRemoteFiles = ((FileTableModel) this.remoteFileList.getModel()).getData();

            int[] selectedRows = this.remoteFileList.getSelectedRows();
            this.selectedRemoteFiles = new ArrayList<>(selectedRows.length);
            for (int i : selectedRows) {
                this.selectedRemoteFiles.add(allRemoteFiles.get(i));
            }

            int currentSelectRow = this.remoteFileList.getSelectedRow();
            if (currentSelectRow != -1) {
                this.lastFile = this.currentRemoteFile;
                this.currentRemoteFile = allRemoteFiles.get(currentSelectRow);
            }
        });
        this.remoteFileList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
        this.remoteFileList.setDragEnabled(true);
        this.remoteFileList.setDropMode(DropMode.ON);
        this.remoteFileList.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDrop()) {
                    return false;
                }

                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    //noinspection unchecked
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : files) {
                        self.uploadFile(file.getAbsolutePath(), self.sftpChannel.file(self.currentRemotePath));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    /**
     * 获取本地文件目录
     * @param path 路径, 为null为空时加载{@link this#USER_HOME}
     */
    public void loadLocal (@Nullable String path) {
        try {
            path = path == null || path.isEmpty() ? USER_HOME : path;
            File file = new File(path);
            if (!file.exists()) {
                Services.message(path + " does not exist!", MessageType.INFO);
                this.setCurrentLocalPath(this.currentLocalPath);
            } else if (!file.isDirectory()) {
                if (file.length() > EDITABLE_FILE_SIZE) {
                    DialogWrapper dialog = new Confirm(new Confirm.Options()
                            .title("This file is too large for text editor")
                            .content("Do you still want to edit it?"));
                    if (dialog.showAndGet()) {
                        this.openFileInEditor(file);
                    }
                } else {
                    this.openFileInEditor(file);
                }
            } else {
                File[] files = file.listFiles();
                if (files == null) {
                    DialogWrapper dialog = new Confirm(
                        new Confirm.Options()
                            .title("It is an unavailable folder!")
                            .content("This folder is not available, do you want to open it in system file manager?")
                    );
                    if (dialog.showAndGet()) {
                        try {
                            Desktop.getDesktop().open(file);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                            Services.message("Failed to open file in system file manager", MessageType.INFO);
                        }
                    }
                    return;
                }

                path = file.getAbsolutePath();

                this.setCurrentLocalPath(path);

                List<FileModel> fileModels = new ArrayList<>(file.length() == 0 ? 1 : (file.length() > Integer.MAX_VALUE ?
                        Integer.MAX_VALUE :
                        Integer.parseInt(String.valueOf(file.length()))
                ));

                // 添加返回上一级目录
                fileModels.add(this.getParentFolder(path, File.separator));

                for (File currentFile : files) {
                    fileModels.add(new FileModel(
                            currentFile.getAbsolutePath(),
                            currentFile.getName(),
                            currentFile.isDirectory(),
                            currentFile.length(),
                            (currentFile.canRead() ? 1 : 0) |
                                    (currentFile.canWrite() ? 2 : 0) |
                                    (currentFile.canExecute() ? 4 : 0)
                    ));
                }

                rerenderFileList(this.localFileList, fileModels);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Services.message(e.getMessage(), MessageType.WARNING);
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

                                // 获取sftpClient
                                try {
                                    // 使用反射获取到 net.schmizz.sshj.sftp.SFTPClient
                                    Class<SshjSftpChannel> sftpChannelClass = SshjSftpChannel.class;

                                    Field field = ReflectionUtil.findFieldInHierarchy(sftpChannelClass, f -> f.getType() == SFTPClient.class);
                                    if (field == null) {
                                        throw new IllegalArgumentException("Unable to upload files!");
                                    }
                                    field.setAccessible(true);
                                    this.sftpClient = (SFTPClient) field.get(this.sftpChannel);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Services.message("Failed to get sftp client for this session, please try it again: "
                                            + e.getMessage(), MessageType.ERROR);
                                }

                                this.loadRemote(this.sftpChannel.getHome());
                            }
                    );
        } catch (Exception e) {
            e.printStackTrace();
            this.triggerDisconnected();
            Services.message(e.getMessage(), MessageType.ERROR);
        }
    }

    /**
     * 获取远程文件目录
     * @param path 默认地址, 为null为空时使用sftp默认文件夹
     */
    public void loadRemote (@Nullable String path) {
        if (this.sftpChannel == null) {
            Services.message("Please connect to server first!", MessageType.INFO);
        } else if (!this.sftpChannel.isConnected()) {
            Services.message("SFTP lost connection, retrying...", MessageType.ERROR);
            this.connectSftp();
        }

        this.remoteFileList.setEnabled(false);
        this.remotePath.setEnabled(false);
        try {
            path = path == null || path.isEmpty() ? this.sftpChannel.getHome() : path;
            RemoteFileObject file = this.sftpChannel.file(path);
            if (!file.exists()) {
                Services.message(path + " does not exist!", MessageType.INFO);
                this.setCurrentRemotePath(this.currentRemotePath);
            } else if (!file.isDir()) {
                // 如果文件小于2M, 则自动下载到缓存目录并进行监听
                if (file.size() > EDITABLE_FILE_SIZE) {
                    DialogWrapper dialog = new Confirm(new Confirm.Options()
                            .title("This file is too large for text editor")
                            .content("Do you still want to download and edit it?"));
                    if (dialog.showAndGet()) {
                        this.downloadFileAndEdit(file);
                    }
                } else {
                    this.downloadFileAndEdit(file);
                }
            } else {
                this.setCurrentRemotePath(path);

                List<RemoteFileObject> files = file.list();
                List<FileModel> fileModels = new ArrayList<>(files.size());

                // 添加返回上一级目录
                fileModels.add(this.getParentFolder(path, SERVER_FILE_SYSTEM_SEPARATOR));

                for (RemoteFileObject f : files) {
                    fileModels.add(new FileModel(
                            // 处理有些文件夹是//开头的
                            this.normalizeRemoteFileObjectPath(f),
                            f.name(), f.isDir(), f.size(), f.getPermissions()
                    ));
                }

                rerenderFileTable(this.remoteFileList, fileModels);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Services.message("Error eccurred while listing remote files: " + e.getMessage(), MessageType.ERROR);
        } finally {
            this.remoteFileList.setEnabled(true);
            this.remotePath.setEnabled(true);
        }
    }

    /**
     * 断开当前连接
     */
    public void disconnect (boolean triggerEvent) {
        if (this.sftpChannel != null && this.sftpChannel.isConnected()) {
            this.sftpChannel.disconnect();
        }

        this.sftpChannel = null;
        this.sftpClient = null;

        if (triggerEvent) {
            this.triggerDisconnected();
        }
    }

    /**
     * 设置当前状态为连接中
     */
    public void triggerConnecting () {
        this.exploreButton.setEnabled(false);
        this.exploreButton.setIcon(AnimatedIcon.Default.INSTANCE);
    }

    /**
     * 设置当前状态为已连接
     */
    public void triggerConnected () {
        this.remotePath.setEnabled(true);
        this.exploreButton.setVisible(false);
        this.exploreButton.setIcon(null);
        this.disconnectButton.setVisible(true);
    }

    /**
     * 设置当前状态为未连接
     */
    public void triggerDisconnected () {
        // 设置远程路径输入框
        this.remotePath.setText("");
        this.remotePath.setEnabled(false);

        this.exploreButton.setEnabled(true);
        this.exploreButton.setVisible(true);
        this.disconnectButton.setVisible(false);

        // 情况远程选择了的文件
        this.selectedRemoteFiles = new ArrayList<>(COLLECTION_SIZE);

        // 清空列表
        if (this.remoteFileList.getModel() instanceof FileTableModel) {
            ((FileTableModel) this.remoteFileList.getModel()).resetData(new ArrayList<>());
        }
        // 清空文件列表
        this.remoteEditingFiles = new HashMap<>(COLLECTION_SIZE);
    }

    /**
     * 设置当前显示的本地路径
     * @param currentLocalPath 本地文件路径
     */
    public void setCurrentLocalPath(String currentLocalPath) {
        this.localPath.setText(currentLocalPath);
        this.currentLocalPath = currentLocalPath;
    }

    /**
     * 谁当前现实的远程路径
     * @param currentRemotePath 远程文件路径
     */
    public void setCurrentRemotePath(String currentRemotePath) {
        this.remotePath.setText(currentRemotePath);
        this.currentRemotePath = currentRemotePath;
    }

    /**
     * 获取上一级目录
     * @param path 当前目录
     */
    private FileModel getParentFolder(String path, String separator) {
        // 添加返回上一级目录
        int lastIndexOfSep = path.lastIndexOf(separator);
        String parentFolder = lastIndexOfSep == -1 || lastIndexOfSep == 0 ? "" : path.substring(0, lastIndexOfSep);
        return new FileModel(parentFolder.isEmpty() ? separator : parentFolder , "..", true, null, null);
    }

    /**
     * 上传文件, 并通知bus
     * @param localFile 本地文件
     * @param remoteFile 线上文件
     */
    private void uploadFile (String localFile, RemoteFileObject remoteFile) {
        // System.out.println("上传" + localFile + ":" + remoteFile.path());

        // 检查当前传输的队列, 存在相同target的, 取消上一个任务
        for (Map.Entry<Runnable, Transfer> entry : Data.TRANSFERRING.entrySet()) {
            Transfer exists = entry.getValue();
            if (exists.getTarget().equals(remoteFile.path())) {
                if (exists.getFuture() != null) {
                    try {
                        exists.getFuture().cancel(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        File file = new File(localFile);

        Transfer transfer = new Transfer();
        transfer.setType(Transfer.Type.UPLOAD);
        transfer.setResult(Transfer.Result.TRANSFERRING);
        transfer.setSource(localFile);
        transfer.setSize(file.length());
        transfer.setTarget(remoteFile.path());

        XFTPExplorerWindow self = XFTPExplorerWindow.this;

        Runnable runnable = () -> {
            try {
                if (!file.exists()) {
                    Services.message("Can't find file " + localFile, MessageType.ERROR);
                    throw new IllegalStateException(localFile + " does not exists!");
                }

                ProgressManager.getInstance().run(new Task.Backgroundable(this.project, "Upload " + localFile + " to " + remoteFile.path()) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(false);
                        try {
                            SFTPFileTransfer fileTransfer = new SFTPFileTransfer(self.sftpClient.getSFTPEngine());
                            fileTransfer.setTransferListener(new TransferListener() {
                                @Override
                                public TransferListener directory(String name) {
                                    return this;
                                }

                                @Override
                                public StreamCopier.Listener file(String name, long size) {
                                    return transferred -> {
                                        double percent = ((double) transferred) / size;
                                        indicator.setFraction(percent);
                                        indicator.setText((Math.round(percent * 100) / 100) + "%");
                                        indicator.setText2("Uploading " + localFile + " to " + remoteFile.path());
                                    };
                                }
                            });

                            String normalizedPath = self.normalizeRemoteFileObjectPath(remoteFile);
                            fileTransfer.upload(file.getAbsolutePath(), normalizedPath);

                            // 如果上传目录和当前目录相同, 则刷新目录
                            if (remoteFile.isDir()) {
                                if (normalizedPath.equals(self.currentRemotePath)) {
                                    self.loadRemote(self.currentRemotePath);
                                }
                            } else {
                                if (self.getParentFolder(normalizedPath, SERVER_FILE_SYSTEM_SEPARATOR)
                                        .getPath()
                                        .equals(self.currentRemotePath)) {
                                    self.loadRemote(self.currentRemotePath);
                                }
                            }

                            // Services.message(remoteFile.path() + " Uploaded", MessageType.INFO);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Services.message("Error occurred while uploading " + localFile + " to " + remoteFile.path() + ", " +
                                    e.getMessage(), MessageType.ERROR);
                        } finally {
                            Runnable r = Maps.getFirstKeyByValue(Data.TRANSFERRING, transfer);
                            if (r != null) Data.TRANSFERRING.remove(r);
                        }
                    }
                });
            } catch (IllegalStateException e) {
                e.printStackTrace();
                Services.message("Error occurred while uploading " + localFile + " to " + remoteFile.path() + ", " +
                        e.getMessage(), MessageType.ERROR);
            } finally {
                Runnable r = Maps.getFirstKeyByValue(Data.TRANSFERRING, transfer);
                if (r != null) Data.TRANSFERRING.remove(r);
            }
        };

        // 添加到线程池队列
        try {
            transfer.setFuture(Services.UPLOAD_POOL.submit(runnable));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Runnable r = Maps.getFirstKeyByValue(Data.TRANSFERRING, transfer);
            if (r != null) Data.TRANSFERRING.remove(r);
        }

        Data.TRANSFERRING.put(runnable, transfer);
        Data.HISTORY.add(transfer);
    }

    /**
     * 下载文件
     * @param file 远程文件
     * @return 本地文件
     */
    private File downloadFile (RemoteFileObject file) {
        if (this.sftpChannel == null || !this.sftpChannel.isConnected()) {
            Services.message("Please start a sftp session first!", MessageType.INFO);
            return null;
        }

        try {
            final File localFile = File.createTempFile("jb-ide-xftp", file.name());
            this.sftpChannel.downloadFileOrDir(file.path(), localFile.getAbsolutePath());
            return localFile;
        } catch (Exception e) {
            e.printStackTrace();
            Services.message("error occurred while downloading file [" + file.path() + "], " + e.getMessage(), MessageType.ERROR);
        }

        return null;
    }

    /**
     * 下载文件并编辑
     * @param file 远程文件信息
     */
    private void downloadFileAndEdit (RemoteFileObject file) {
        if (file.isDir()) {
            throw new IllegalArgumentException("Can not edit a folder!");
        }

        // 如果当前远程文件已经在编辑器中打开了, 则关闭之前的
        RemoteFileObject existsRemoteFile = this.remoteEditingFiles
                .keySet().stream()
                .filter(rf -> rf.path().equals(file.path()))
                .findFirst()
                .orElse(null);
        if (existsRemoteFile != null) {
            File oldCachedFile = new File(this.remoteEditingFiles.get(existsRemoteFile));
            if (oldCachedFile.exists()) {
                DialogWrapper dialog = new Confirm(new Confirm.Options()
                        .title("This file is editing")
                        .okText("Replace")
                        .cancelText("Open")
                        .content("Do you want to replace current editing file? \n" +
                                "Press \"Open\" to open/focus an/the editor for/of existing file. \n" +
                                "Press \"Replace\" to discard downloaded file and re-download the file from remote.")
                );
                if (!dialog.showAndGet()) {
                    this.openFileInEditor(oldCachedFile);
                    return;
                } else {
                    this.remoteEditingFiles.remove(existsRemoteFile);
                }
            }
        }

        this.panelWrapper.setEnabled(false);
        File localFile = this.downloadFile(file);
        this.panelWrapper.setEnabled(true);
        if (localFile == null) {
            return;
        }

        this.openFileInEditor(localFile);

        // 加入文件监听队列
        this.remoteEditingFiles.put(file, localFile.getAbsolutePath());
    }

    /**
     * 打开文件
     * @param file 文件
     */
    private void openFileInEditor (@NotNull File file) {
        FileEditorManager.getInstance(this.project).openTextEditor(
                new OpenFileDescriptor(
                        this.project,
                        Objects.requireNonNull(LocalFileSystem.getInstance().findFileByIoFile(file)),
                        0
                ),
                true
        );
    }

    /**
     * 格式化远程文件的路径, 因为有的文件开头是//
     * @param file 远程文件信息
     * @return 格式化后的文件路径
     */
    private String normalizeRemoteFileObjectPath (RemoteFileObject file) {
        return file.path().startsWith("//") ? file.path().substring(1) : file.path();
    }

}
