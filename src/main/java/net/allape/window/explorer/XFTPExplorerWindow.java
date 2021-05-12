package net.allape.window.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import com.intellij.openapi.fileEditor.impl.text.FileDropHandler;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.remote.RemoteConnectionType;
import com.intellij.remote.RemoteCredentials;
import com.intellij.ssh.ConnectionBuilder;
import com.intellij.ssh.RemoteCredentialsUtil;
import com.intellij.ssh.RemoteFileObject;
import com.intellij.ssh.SshTransportException;
import com.intellij.ssh.channels.SftpChannel;
import com.intellij.ssh.impl.sshj.channels.SshjSftpChannel;
import com.intellij.ui.content.Content;
import com.intellij.util.ReflectionUtil;
import com.jetbrains.plugins.remotesdk.console.RemoteDataProducer;
import com.jetbrains.plugins.remotesdk.console.SshTerminalDirectRunner;
import icons.TerminalIcons;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.internal.functions.Functions;
import net.allape.action.ActionToolbarFastEnableAnAction;
import net.allape.bus.HistoryTopicHandler;
import net.allape.bus.Services;
import net.allape.bus.Windows;
import net.allape.component.FileTable;
import net.allape.component.MemoComboBox;
import net.allape.exception.TransferCancelledException;
import net.allape.model.FileModel;
import net.allape.model.FileTransferHandler;
import net.allape.model.FileTransferable;
import net.allape.model.Transfer;
import net.allape.util.Maps;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPFileTransfer;
import net.schmizz.sshj.xfer.TransferListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalView;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class XFTPExplorerWindow extends XFTPExplorerUI {

    // 传输历史记录topic的publisher
    private final HistoryTopicHandler historyTopicHandler;
    // 传输历史
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Transfer> HISTORY = new ArrayList<>(100);
    // 传输中的
    private final List<Transfer> TRANSFERRING = new ArrayList<>(100);

    // 窗口content
    private Content content = null;

    // 上一次选择文件
    private FileModel lastFile = null;

    // 当前选中的本地文件
    private FileModel currentLocalFile = null;

    // 当前配置
    private RemoteCredentials credentials = null;
    // 当前配置的连接创建者
    private ConnectionBuilder connectionBuilder = null;
    // 当前开启的channel
    private SftpChannel sftpChannel = null;
    // 当前channel中的sftp client
    private SFTPClient sftpClient = null;

    // 当前选中的远程文件
    private FileModel currentRemoteFile = null;

    // 修改中的远程文件, 用于文件修改后自动上传 key: remote file, value: local file
    private Map<RemoteFileObject, String> remoteEditingFiles = new HashMap<>(COLLECTION_SIZE);

    // 建立连接
    private final ActionToolbarFastEnableAnAction explore = new ActionToolbarFastEnableAnAction(
            this.remoteActionToolBar,
            "Start New Session", "Start a sftp session",
            AllIcons.Webreferences.Server
    ) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(false);
            XFTPExplorerWindow.this.connectSftp();
        }
    };
    // 刷新
    private final ActionToolbarFastEnableAnAction reload = new ActionToolbarFastEnableAnAction(
            this.remoteActionToolBar,
            "Reload", "Reload current folder",
            AllIcons.Actions.Refresh
    ) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            XFTPExplorerWindow.this.reloadRemote();
        }
    };
    // 断开连接
    private final ActionToolbarFastEnableAnAction suspend = new ActionToolbarFastEnableAnAction(
            this.remoteActionToolBar,
            "Disconnect", "Disconnect from sftp server",
            AllIcons.Actions.Suspend
    ) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            XFTPExplorerWindow self = XFTPExplorerWindow.this;

            if (MessageDialogBuilder
                    .yesNo("Disconnecting", "Do you really want to close this session?")
                    .asWarning()
                    .yesText("Disconnect")
                    .ask(self.project)) {
                self.disconnect(true);
            }
        }
    };
    // 命令行打开
    private final ActionToolbarFastEnableAnAction newTerminal = new ActionToolbarFastEnableAnAction(
            this.remoteActionToolBar,
            "Open In Terminal", "Open current folder in ssh terminal",
            TerminalIcons.OpenTerminal_13x13
    ) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            XFTPExplorerWindow self = XFTPExplorerWindow.this;

            TerminalTabState state = new TerminalTabState();
            state.myWorkingDirectory = self.remotePath.getMemoItem();
            TerminalView.getInstance(self.project).createNewSession(new SshTerminalDirectRunner(self.project, self.credentials, Charset.defaultCharset()), state);
        }
    };

    public XFTPExplorerWindow(Project project, ToolWindow toolWindow) {
        super(project, toolWindow);

        this.historyTopicHandler = this.project.getMessageBus().syncPublisher(HistoryTopicHandler.HISTORY_TOPIC);

        final XFTPExplorerWindow self = XFTPExplorerWindow.this;
        this.setCurrentLocalPath(Objects.requireNonNull(this.project.getBasePath()));
        // 初始化文件监听
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
                                self.application.executeOnPooledThread(() -> self.transfer(new File(localFile), remoteFile, Transfer.Type.UPLOAD)
                                        .subscribe(Functions.emptyConsumer(), Throwable::printStackTrace)
                                );
                            }
                        }
                    }
                }
            }
        });

        this.initUI();
    }

    @Override
    public void dispose() {
        super.dispose();

        // 关闭连接
        this.disconnect(true);
    }

    /**
     * 初始化UI行为
     */
    protected void initUI () {
        super.initUI();

        final XFTPExplorerWindow self = XFTPExplorerWindow.this;

        this.localActionGroup.addAll(
                new AnAction("Refresh", "Refresh current folder", AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        self.reloadLocal();
                    }
                },
                new AnAction("Open In Finder/Explorer", "Display folder in system file manager", AllIcons.Actions.MenuOpen) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    String path = self.localPath.getMemoItem();
                    try {
                        Desktop.getDesktop().open(new File(path));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                        Services.message("Failed to open \"" + path + "\"", MessageType.ERROR);
                    }
                }
            }
        );
        this.localPath.addActionListener(e -> {
            String path = this.localPath.getMemoItem();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            try {
                File file = new File(path);
                if (!file.exists()) {
                    Services.message(path + " does not exist!", MessageType.INFO);
                    // 获取第二个历史, 不存在时加载项目路径
                    MemoComboBox.MemoComboBoxPersistenceModel<String> lastPath = this.localPath.getItemAt(1);
                    if (lastPath != null && lastPath.getMemo() != null) {
                        this.setCurrentLocalPath(lastPath.getMemo());
                    } else {
                        this.setCurrentLocalPath(Objects.requireNonNull(this.project.getBasePath()));
                    }
                } else if (!file.isDirectory()) {
                    // 加载父文件夹
                    this.setCurrentLocalPath(file.getParent());
                    if (file.length() > EDITABLE_FILE_SIZE) {
                        if (MessageDialogBuilder
                                .yesNo("This file is too large for text editor", "Do you still want to edit it?")
                                .asWarning()
                                .yesText("Edit it")
                                .ask(this.project)) {
                            this.openFileInEditor(file);
                        }
                    } else {
                        this.openFileInEditor(file);
                    }
                } else {
                    File[] files = file.listFiles();
                    if (files == null) {
                        if (MessageDialogBuilder
                                .yesNo("It is an unavailable folder!", "This folder is not available, do you want to open it in system file manager?")
                                .asWarning()
                                .yesText("Open")
                                .ask(this.project)) {
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
                                (currentFile.canRead() ? 0b100 : 0) |
                                        (currentFile.canWrite() ? 0b10 : 0) |
                                        (currentFile.canExecute() ? 0b1 : 0),
                                true
                        ));
                    }

                    this.localFileList.resetData(fileModels);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Services.message(ex.getMessage(), MessageType.WARNING);
            }
        });
        this.localFileList.getSelectionModel().addListSelectionListener(e -> {
            List<FileModel> allRemoteFiles = this.localFileList.getModel().getData();

            int currentSelectRow = this.localFileList.getSelectedRow();
            if (currentSelectRow != -1) {
                this.lastFile = this.currentLocalFile;
                this.currentLocalFile = allRemoteFiles.get(currentSelectRow);
            }
        });
        // 监听双击, 双击后打开文件或文件夹
        this.localFileList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    long now = System.currentTimeMillis();
                    if (self.lastFile == self.currentLocalFile && now - self.clickWatcher < DOUBLE_CLICK_INTERVAL) {
                        self.setCurrentLocalPath(self.currentLocalFile.getPath());
                    }

                    self.clickWatcher = now;
                }
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
        this.localFileList.setDragEnabled(true);
        this.localFileList.setTransferHandler(new FileTransferHandler<File>() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                List<FileModel> fileModels = self.getSelectedFileList(self.localFileList);
                List<File> files = new ArrayList<>(fileModels.size());
                for (FileModel model: fileModels) {
                    files.add(new File(model.getPath()));
                }
                return new FileTransferable<>(files, DataFlavor.javaFileListFlavor);
            }
            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDrop()) {
                    return false;
                }
                return support.isDataFlavorSupported(remoteFileListFlavor);
            }
            @Override
            public boolean importData(TransferSupport support) {
                try {
                    //noinspection unchecked
                    List<RemoteFileObject> files = (List<RemoteFileObject>) support.getTransferable().getTransferData(remoteFileListFlavor);
                    final String currentLocalPath = self.localPath.getMemoItem();
                    for (RemoteFileObject file : files) {
                        self.application.executeOnPooledThread(() -> self.transfer(
                                new File(currentLocalPath + File.separator + file.name()),
                                file,
                                Transfer.Type.DOWNLOAD
                        ).subscribe(Functions.emptyConsumer(), Throwable::printStackTrace));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        this.remotePath.setEnabled(false);
        this.remotePath.addActionListener(e -> {
            String remoteFilePath = this.remotePath.getMemoItem();

            if (remoteFilePath == null) {
                return;
            }

            this.application.executeOnPooledThread(() -> {
                if (this.sftpChannel == null) {
                    Services.message("Please connect to server first!", MessageType.INFO);
                } else if (!this.sftpChannel.isConnected()) {
                    Services.message("SFTP lost connection, retrying...", MessageType.ERROR);
                    this.connectSftp();
                }

                this.remoteFileList.setEnabled(false);
                this.remotePath.setEnabled(false);
                // 是否接下来加载父文件夹
                String loadParent = null;
                try {
                    String path = remoteFilePath.isEmpty() ? SERVER_FILE_SYSTEM_SEPARATOR : remoteFilePath;
                    RemoteFileObject file = this.sftpChannel.file(path);
                    path = file.path();
                    if (!file.exists()) {
                        Services.message(path + " does not exist!", MessageType.INFO);
                        MemoComboBox.MemoComboBoxPersistenceModel<String> lastPath = this.remotePath.getItemAt(1);
                        if (lastPath != null && lastPath.getMemo() != null) {
                            this.setCurrentRemotePath(lastPath.getMemo());
                        } else {
                            this.setCurrentRemotePath(this.sftpChannel.getHome());
                        }
                    } else if (!file.isDir()) {
                        this.downloadFileAndEdit(file);
                        loadParent = this.getParentFolderPath(file.path(), SERVER_FILE_SYSTEM_SEPARATOR);
                    } else {
                        List<RemoteFileObject> files = file.list();
                        List<FileModel> fileModels = new ArrayList<>(files.size());

                        // 添加返回上一级目录
                        fileModels.add(this.getParentFolder(path, SERVER_FILE_SYSTEM_SEPARATOR));

                        for (RemoteFileObject f : files) {
                            fileModels.add(new FileModel(
                                    // 处理有些文件夹是//开头的
                                    this.normalizeRemoteFileObjectPath(f),
                                    f.name(), f.isDir(), f.size(), f.getPermissions(), false
                            ));
                        }

                        this.application.invokeLater(() -> this.remoteFileList.resetData(fileModels));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Services.message("Error occurred while listing remote files: " + ex.getMessage(), MessageType.ERROR);
                } finally {
                    this.remoteFileList.setEnabled(true);
                    this.remotePath.setEnabled(true);

                    if (loadParent != null) {
                        // 加载父文件夹
                        this.setCurrentRemotePath(loadParent);
                    }
                }
            });
        });

        this.remoteActionGroup.addAll(
                explore,
                reload,
                suspend,
                newTerminal
        );
        this.reload.setEnabled(false);
        this.suspend.setEnabled(false);
        this.newTerminal.setEnabled(false, true);

        this.remoteFileList.getSelectionModel().addListSelectionListener(e -> {
            List<FileModel> allRemoteFiles = this.remoteFileList.getModel().getData();

            int currentSelectRow = this.remoteFileList.getSelectedRow();
            if (currentSelectRow != -1) {
                this.lastFile = this.currentRemoteFile;
                this.currentRemoteFile = allRemoteFiles.get(currentSelectRow);
            }
        });
        this.remoteFileList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    long now = System.currentTimeMillis();
                    if (self.lastFile == self.currentRemoteFile && now - self.clickWatcher < DOUBLE_CLICK_INTERVAL) {
                        self.setCurrentRemotePath(self.currentRemoteFile.getPath());
                    }

                    self.clickWatcher = now;
                }
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
        this.remoteFileList.setTransferHandler(new FileTransferHandler<RemoteFileObject>() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                return new FileTransferable<>(self.getSelectedRemoteFileList(), remoteFileListFlavor);
            }
            @Override
            public boolean canImport(TransferSupport support) {
                if (self.sftpChannel == null || !self.sftpChannel.isConnected()) {
                    return false;
                } else if (!support.isDrop()) {
                    return false;
                }

                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
            @Override
            public boolean importData(TransferSupport support) {
                try {
                    //noinspection unchecked
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    final String currentRemotePath = self.remotePath.getMemoItem();
                    for (File file : files) {
                        self.application.executeOnPooledThread(() -> self.transfer(
                                file,
                                self.sftpChannel.file(currentRemotePath + SERVER_FILE_SYSTEM_SEPARATOR + file.getName()),
                                Transfer.Type.UPLOAD
                        ).subscribe(Functions.emptyConsumer(), Throwable::printStackTrace));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        JBMenuItem remoteFileListPopupMenuDelete = new JBMenuItem("rm -Rf");
        remoteFileListPopupMenuDelete.addActionListener(e -> {
            if (!MessageDialogBuilder
                    .yesNo("Delete Confirm", "This operation is irreversible, continue?")
                    .asWarning()
                    .yesText("Cancel")
                    .noText("Continue")
                    .ask(this.project)) {
                List<RemoteFileObject> files = self.getSelectedRemoteFileList();
                this.application.invokeLater(() -> {
                    self.lockRemoteUIs();
                    self.application.executeOnPooledThread(() -> {
                        // 开启一个ExecChannel来删除非空的文件夹
                        try {
                            for (RemoteFileObject file : files) {
                                try {
                                    this.connectionBuilder.execBuilder("rm -Rf " + file.path()).execute().waitFor();
                                    // file.rm();
                                } catch (Exception err) {
                                    err.printStackTrace();
                                    Services.message("Error occurred while delete file: " + file.path(), MessageType.ERROR);
                                }
                            }
                        } catch (Exception err) {
                            err.printStackTrace();
                            Services.message("Can't delete file or folder right now, please try it later", MessageType.ERROR);
                        } finally {
                            self.application.invokeLater(() -> {
                                self.unlockRemoteUIs();
                                // 刷新当前页面
                                self.reloadRemote();
                            });
                        }
                    });
                });
            }
        });

        final JBPopupMenu remoteFileListPopupMenu = new JBPopupMenu();
        remoteFileListPopupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                remoteFileListPopupMenuDelete.setEnabled(self.getSelectedRemoteFileList().size() != 0);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) { }
        });
        remoteFileListPopupMenu.add(remoteFileListPopupMenuDelete);
        this.remoteFileList.setComponentPopupMenu(remoteFileListPopupMenu);
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
            new RemoteDataProducer()
                .withProject(this.project)
                .produceRemoteData(
                    RemoteConnectionType.SSH_CONFIG,
                    null,
                    "",
                    data -> {
                        this.credentials = data;
                        this.triggerConnecting();
                        this.disconnect(false);

                        this.application.executeOnPooledThread(() -> {
                            // com.jetbrains.plugins.remotesdk.tools.RemoteTool.startRemoteProcess
                            //noinspection UnstableApiUsage
                            this.connectionBuilder = RemoteCredentialsUtil.connectionBuilder(
                                    this.credentials, this.project,
                                    ProgressManager.getGlobalProgressIndicator(), true
                            ).withConnectionTimeout(60L);
                            XFTPExplorerWindow self = this;

                            ProgressManager.getInstance().run(new Task.Backgroundable(
                                    this.project,
                                    "connecting to " + this.credentials.getUserName() + "@" + this.credentials.getHost(),
                                    // 暂时不允许取消
                                    false
                            ) {
                                // 是否已被取消
                                private boolean cancelled = false;
                                @Override
                                public void run(@NotNull ProgressIndicator indicator) {
                                    indicator.setIndeterminate(true);
                                    try {
                                        self.sftpChannel = self.connectionBuilder.openSftpChannel();
                                        self.triggerConnected();

                                        try {
                                            // 获取sftpClient
                                            // 使用反射获取到 net.schmizz.sshj.sftp.SFTPClient
                                            Class<SshjSftpChannel> sftpChannelClass = SshjSftpChannel.class;

                                            Field field = ReflectionUtil.findFieldInHierarchy(sftpChannelClass, f -> f.getType() == SFTPClient.class);
                                            if (field == null) {
                                                throw new IllegalArgumentException("Unable to upload files!");
                                            }
                                            field.setAccessible(true);
                                            self.sftpClient = (SFTPClient) field.get(self.sftpChannel);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            Services.message("Failed to get sftp client for this session, please try it again: "
                                                    + e.getMessage(), MessageType.ERROR);
                                        }

                                        self.application.invokeLater(() -> self.setCurrentRemotePath(self.sftpChannel.getHome()));
                                    } catch (SshTransportException e) {
                                        if (!cancelled) {
                                            Services.message("连接失败, 请重试", MessageType.WARNING);
                                        }
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onCancel() {
                                    super.onCancel();
                                    this.cancelled = true;
                                }
                            });
                        });
                    }
                );
        } catch (Exception e) {
            e.printStackTrace();
            this.triggerDisconnected();
            Services.message(e.getMessage(), MessageType.ERROR);
        }
    }

    /**
     * 断开当前连接
     */
    public void disconnect (boolean triggerEvent) {
        // 断开连接
        if (this.sftpChannel != null && this.sftpChannel.isConnected()) {
            try {
                this.sftpChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.connectionBuilder = null;
        this.sftpChannel = null;
        this.sftpClient = null;

        if (this.content != null) {
            this.content.setDisplayName(Windows.WINDOW_DEFAULT_NAME);
        }

        if (triggerEvent) {
            this.triggerDisconnected();
        }
    }

    /**
     * 设置当前状态为连接中
     */
    public void triggerConnecting () {
        this.application.invokeLater(() -> this.explore.setEnabled(false, true));
    }

    /**
     * 设置当前状态为已连接
     */
    public void triggerConnected () {
        this.application.invokeLater(() -> {
            this.remotePath.setEnabled(true);

            this.explore.setEnabled(false);
            this.reload.setEnabled(true);
            this.suspend.setEnabled(true);
            this.newTerminal.setEnabled(true);

            if (this.content != null && this.credentials != null) {
                this.content.setDisplayName(
                        this.credentials.getUserName() +
                        "@" +
                        this.credentials.getHost()
                );
            }
        });
    }

    /**
     * 设置当前状态为未连接
     */
    public void triggerDisconnected () {
        this.application.invokeLater(() -> {
            // 设置远程路径输入框
            this.remotePath.setItem(null);
            this.remotePath.setEnabled(false);

            this.explore.setEnabled(true);
            this.reload.setEnabled(false);
            this.suspend.setEnabled(false);
            this.newTerminal.setEnabled(false, true);

            // 清空列表
            if (this.remoteFileList.getModel() != null) {
                this.remoteFileList.getModel().resetData(new ArrayList<>());
            }
            // 清空文件列表
            this.remoteEditingFiles = new HashMap<>(COLLECTION_SIZE);
        });
    }

    /**
     * 设置当前显示的本地路径
     * @param currentLocalPath 本地文件路径
     */
    public void setCurrentLocalPath(@NotNull String currentLocalPath) {
        boolean fireEventManually = currentLocalPath.equals(this.localPath.getMemoItem());
        this.localPath.push(currentLocalPath);
        if (fireEventManually) {
            this.localPath.actionPerformed(null);
        }
    }

    /**
     * 刷新本地资源
     */
    public void reloadLocal() {
        this.setCurrentLocalPath(this.localPath.getMemoItem());
    }

    /**
     * 谁当前现实的远程路径
     * @param currentRemotePath 远程文件路径
     */
    public void setCurrentRemotePath(@NotNull String currentRemotePath) {
        boolean fireEventManually = currentRemotePath.equals(this.remotePath.getMemoItem());
        this.remotePath.push(currentRemotePath);
        if (fireEventManually) {
            this.remotePath.actionPerformed(null);
        }
    }

    /**
     * 刷新远程资源
     */
    public void reloadRemote() {
        this.setCurrentRemotePath(this.remotePath.getMemoItem());
    }

    /**
     * 获取上一级目录
     * @param path 当前目录
     * @param separator 文件系统分隔符
     */
    private FileModel getParentFolder(String path, String separator) {
        return new FileModel(this.getParentFolderPath(path, separator), "..", true, 0, 0, false);
    }

    /**
     * 获取上一级目录
     * @param path 当前目录
     * @param separator 文件系统分隔符
     */
    private String getParentFolderPath(String path, String separator) {
        // 添加返回上一级目录
        int lastIndexOfSep = path.lastIndexOf(separator);
        String parentFolder = lastIndexOfSep == -1 || lastIndexOfSep == 0 ? "" : path.substring(0, lastIndexOfSep);
        return parentFolder.isEmpty() ? separator : parentFolder;
    }

    /**
     * 下载文件并编辑
     * @param remoteFile 远程文件信息
     */
    private void downloadFileAndEdit (RemoteFileObject remoteFile) {
        if (remoteFile.isDir()) {
            throw new IllegalArgumentException("Can not edit a folder!");
        }

        this.application.invokeLater(() -> {
            // 如果文件小于2M, 则自动下载到缓存目录并进行监听
            if (remoteFile.size() > EDITABLE_FILE_SIZE) {
                if (!MessageDialogBuilder
                        .yesNo("This file is too large for text editor", "Do you still want to download and edit it?")
                        .asWarning()
                        .yesText("Do it")
                        .ask(this.project)) {
                    return;
                }
            }

            // 如果当前远程文件已经在编辑器中打开了, 则关闭之前的
            RemoteFileObject existsRemoteFile = this.remoteEditingFiles
                    .keySet().stream()
                    .filter(rf -> rf.path().equals(remoteFile.path()))
                    .findFirst()
                    .orElse(null);
            if (existsRemoteFile != null) {
                File oldCachedFile = new File(this.remoteEditingFiles.get(existsRemoteFile));
                if (oldCachedFile.exists()) {
                    if (MessageDialogBuilder
                            .yesNo("This file is editing", "Do you want to replace current editing file? \n\n" +
                                    "\"Open\" to reopen/focus the existing file. \n" +
                                    "\"Replace\" to re-download the file from remote and open it.")
                            .asWarning()
                            .noText("Replace")
                            .yesText("Open")
                            .ask(this.project)) {
                        this.openFileInEditor(oldCachedFile);
                        return;
                    } else {
                        this.remoteEditingFiles.remove(existsRemoteFile);
                    }
                }
            }

            try {
                File localFile = File.createTempFile("jb-ide-xftp-", "." + remoteFile.name());
                this.application.executeOnPooledThread(() ->
                        this.transfer(localFile, remoteFile, Transfer.Type.DOWNLOAD).subscribe(t -> {
                            this.openFileInEditor(localFile);
                            // 加入文件监听队列
                            this.remoteEditingFiles.put(remoteFile, localFile.getAbsolutePath());
                        }, Throwable::printStackTrace));
            } catch (IOException e) {
                e.printStackTrace();
                Services.message("Unable to create cache file: " + e.getMessage(), MessageType.ERROR);
            }
        });
    }

    /**
     * 打开文件 <br/>
     * 参考: {@link FileDropHandler#openFiles(com.intellij.openapi.project.Project, java.util.List, com.intellij.openapi.fileEditor.impl.EditorWindow)}
     * @param file 文件
     */
    @SuppressWarnings("JavadocReference")
    private void openFileInEditor (@NotNull File file) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        if (virtualFile == null) {
            Services.message(file.getAbsolutePath() + " does not exists!", MessageType.WARNING);
            return;
        }
        NonProjectFileWritingAccessProvider.allowWriting(Collections.singletonList(virtualFile));

        this.application.invokeLater(() -> FileEditorManager.getInstance(this.project).openTextEditor(
                new OpenFileDescriptor(
                        this.project,
                        virtualFile,
                        0
                ),
                true
        ));
    }

    /**
     * 格式化远程文件的路径, 因为有的文件开头是//
     * @param file 远程文件信息
     * @return 格式化后的文件路径
     */
    private String normalizeRemoteFileObjectPath (RemoteFileObject file) {
        return file.path().startsWith("//") ? file.path().substring(1) : file.path();
    }

    /**
     * 获取当前选中了的远程文件列表
     * @return 远程文件列表
     */
    private List<RemoteFileObject> getSelectedRemoteFileList () {
        return this.getSelectedFileList(this.remoteFileList)
                .stream()
                .map(fileModel -> this.sftpChannel.file(fileModel.getPath()))
                .collect(Collectors.toList());
    }

    /**
     * 获取当前选中了的远程文件列表
     * @param fileTable 要获取选中文件的文件列表
     * @return 远程文件列表
     */
    private List<FileModel> getSelectedFileList (FileTable fileTable) {
        int[] rows = fileTable.getSelectedRows();
        List<FileModel> files = new ArrayList<>(rows.length);

        if (rows.length == 0) {
            return files;
        }

        List<FileModel> fileModels = fileTable.getModel().getData();
        for (int row : rows) {
            if (row == 0) continue;
            files.add(fileModels.get(row));
        }

        return files;
    }

    // region 上传下载

    /**
     * 传输文件
     * @param localFile 本地文件路径 {@link File}
     * @param remoteFile 远程文件 {@link SftpChannel#file(String)}
     * @param type 传输类型
     */
    private synchronized Observable<Transfer> transfer (File localFile, RemoteFileObject remoteFile, Transfer.Type type) {
        if (this.sftpChannel == null || !this.sftpChannel.isConnected()) {
            Services.message("Please start a sftp session first!", MessageType.INFO);
            throw new IllegalStateException("No SFTP session available!");
        }

        // 格式化远程路径
        final String normalizedRemotePath = this.sftpChannel.getSshSession().getHost() + ":" + this.normalizeRemoteFileObjectPath(remoteFile);
        // 远程路径
        final String remoteFilePath = remoteFile.path();
        // 本地绝对路径
        final String localFileAbsPath = localFile.getAbsolutePath();

        // 检查当前传输的队列, 存在相同target的, 取消上一个任务
        for (Transfer exists : TRANSFERRING) {
            // 移除非运行中的内容
            if (exists == null || exists.getResult() != Transfer.Result.TRANSFERRING) {
                this.application.invokeLater(() -> TRANSFERRING.remove(exists));
            }
            // 标记相同目标内容为已取消
            else if (
                    type == exists.getType() &&
                            exists.getTarget().equals(type == Transfer.Type.UPLOAD ? normalizedRemotePath : localFileAbsPath)
            ) {
                exists.setResult(Transfer.Result.CANCELLED);
            }
        }

        // 设置传输对象
        Transfer transfer = new Transfer();
        transfer.setType(type);
        transfer.setResult(Transfer.Result.TRANSFERRING);
        transfer.setSize(type == Transfer.Type.UPLOAD ? localFile.length() : remoteFile.size());
        transfer.setSource(type == Transfer.Type.UPLOAD ? localFileAbsPath : normalizedRemotePath);
        transfer.setTarget(type == Transfer.Type.UPLOAD ? normalizedRemotePath : localFileAbsPath);

        TRANSFERRING.add(transfer);
        HISTORY.add(transfer);
        this.historyTopicHandler.before(HistoryTopicHandler.HAction.RERENDER);

        XFTPExplorerWindow self = this;

        return Observable.create(sub -> {
            Observable<Transfer> transferring = Observable.create(emitter -> {
                if (type == Transfer.Type.UPLOAD) {
                    if (!localFile.exists()) {
                        String e = "Can't find local file " + localFileAbsPath;
                        Services.message(e, MessageType.ERROR);
                        sub.onError(new IllegalArgumentException(e));
                        return;
                    }
                } else {
                    if (!remoteFile.exists()) {
                        String e = "Can't find remote file " + remoteFilePath;
                        Services.message(e, MessageType.ERROR);
                        sub.onError(new IllegalArgumentException(e));
                        return;
                    }
                }

                try {
                    ProgressManager.getInstance().run(new Task.Backgroundable(this.project, type == Transfer.Type.UPLOAD ? "Uploading" : "Downloading") {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            indicator.setIndeterminate(type == Transfer.Type.UPLOAD ? localFile.isDirectory() : remoteFile.isDir());
                            try {
                                SFTPFileTransfer fileTransfer = new SFTPFileTransfer(self.sftpClient.getSFTPEngine());
                                fileTransfer.setTransferListener(new TransferListener() {
                                    @Override
                                    public TransferListener directory(String name) {
                                        return this;
                                    }

                                    @Override
                                    public StreamCopier.Listener file(String name, long size) {
                                        String total = FileTable.FileTableModel.byteCountToDisplaySize(size);
                                        return transferred -> {
                                            if (indicator.isCanceled() || transfer.getResult() == Transfer.Result.CANCELLED) {
                                                throw new TransferCancelledException("Operation cancelled!");
                                            }

                                            transfer.setTransferred(transferred);
                                            self.historyTopicHandler.before(HistoryTopicHandler.HAction.RERENDER);

                                            indicator.setText2((type == Transfer.Type.UPLOAD ? "Uploading" : "Downloading") + " " +
                                                    transfer.getSource() + " to " + transfer.getTarget());
                                            double percent = ((double) transferred) / size;

                                            // 文件夹的不显示百分比进度
                                            if (!indicator.isIndeterminate()) {
                                                indicator.setFraction(percent);
                                            }

                                            indicator.setText((Math.round(percent * 10000) / 100) + "% " +
                                                    FileTable.FileTableModel.byteCountToDisplaySize(transferred) + "/" + total);
                                        };
                                    }
                                });

                                // 开始上传
                                if (type == Transfer.Type.UPLOAD) {
                                    fileTransfer.upload(localFileAbsPath, remoteFilePath);
                                } else {
                                    fileTransfer.download(remoteFilePath, localFileAbsPath);
                                }

                                // 如果上传目录和当前目录相同, 则刷新目录
                                if (type == Transfer.Type.UPLOAD) {
                                    String currentRemotePath = self.remotePath.getMemoItem();
                                    if (remoteFile.isDir()) {
                                        if (transfer.getTarget().equals(currentRemotePath)) {
                                            self.setCurrentRemotePath(currentRemotePath);
                                        }
                                    } else {
                                        if (self.getParentFolderPath(transfer.getTarget(), SERVER_FILE_SYSTEM_SEPARATOR)
                                                .equals(currentRemotePath)) {
                                            self.setCurrentRemotePath(currentRemotePath);
                                        }
                                    }
                                } else {
                                    String currentLocalPath = self.localPath.getMemoItem();
                                    if (localFile.isDirectory()) {
                                        if (transfer.getTarget().equals(currentLocalPath)) {
                                            self.setCurrentLocalPath(currentLocalPath);
                                        }
                                    } else {
                                        if (self.getParentFolderPath(transfer.getTarget(), SERVER_FILE_SYSTEM_SEPARATOR)
                                                .equals(currentLocalPath)) {
                                            self.setCurrentLocalPath(currentLocalPath);
                                        }
                                    }
                                }

                                emitter.onNext(transfer);
                                emitter.onComplete();
                            } catch (TransferCancelledException e) {
                                emitter.onError(e);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Services.message("Error occurred while transferring " + transfer.getSource() + " to " + transfer.getTarget() + ", " +
                                        e.getMessage(), MessageType.ERROR);
                                emitter.onError(e);
                            }
                        }
                        @Override
                        public void onCancel() {
                            super.onCancel();
                            this.setCancelText("Cancelling...");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Services.message("Error occurred while starting to transfer " + transfer.getSource() + " to " + transfer.getTarget() + ", " +
                            e.getMessage(), MessageType.ERROR);
                    emitter.onError(e);

                }
            });

            //noinspection ResultOfMethodCallIgnored
            transferring.subscribe(ignore -> {
                transfer.setResult(Transfer.Result.SUCCESS);
                sub.onNext(transfer);
                sub.onComplete();
                // 是上传至当前目录则刷新
                if (remoteFilePath.startsWith(self.remotePath.getMemoItem())) {
                    self.application.invokeLater(self::reloadRemote);
                }
            }, e -> {
                transfer.setResult(Transfer.Result.FAIL);
                transfer.setException(e.getMessage());
                sub.onError(e);
            }, () -> {
                sub.onComplete();
                TRANSFERRING.remove(transfer);
                this.historyTopicHandler.before(HistoryTopicHandler.HAction.RERENDER);
            });
        });
    }

    // endregion

    // region getter / setter

    public void setContent(Content content) {
        if (content != null) {
            this.content = content;
            new XFTPExplorerWindowTabCloseListener(this.content, this.project, this);
        }
    }

    public SFTPClient getSftpClient() {
        return sftpClient;
    }

    // endregion

}
