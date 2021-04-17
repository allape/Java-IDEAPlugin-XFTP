package net.allape.window.explorer;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.ContentManagerEvent;
import net.allape.model.FileModel;
import net.allape.util.FixedList;
import net.allape.util.Grids;
import net.allape.window.XFTPWindow;
import net.allape.component.FileTable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class XFTPExplorerUI extends XFTPWindow {

    public static final String LOCAL_HISTORY_PERSISTENCE_KEY = "xftp.persistence.local-history";
    public static final String REMOTE_HISTORY_PERSISTENCE_KEY = "xftp.persistence.remote-history";
    public static final int HISTORY_MAX_COUNT = 10;

    protected JPanel panelWrapper = new JPanel(new BorderLayout());
    protected JBSplitter splitter = new OnePixelSplitter("xftp-main-window", .5f);

    @SuppressWarnings("rawtypes")
    protected JBPanel localWrapper = new JBPanel(new GridBagLayout());
    protected ComboBox<String> localPath = new ComboBox<>();
    protected FileTable localFileList = new FileTable();
    protected JBScrollPane localFileListWrapper = new JBScrollPane(localFileList);

    @SuppressWarnings("rawtypes")
    protected JBPanel remoteWrapper = new JBPanel(new GridBagLayout());
    @SuppressWarnings("rawtypes")
    protected JBPanel remoteTopWrapper = new JBPanel(new GridBagLayout());
    protected ComboBox<String> remotePath = new ComboBox<>();
    protected JButton exploreButton = new JButton("Explorer");
    protected JButton disconnectButton = new JButton("Disconnect");
    protected JButton newTerminalSessionButton = new JButton("TTY");
    protected FileTable remoteFileList = new FileTable();
    protected JBScrollPane remoteFileListWrapper = new JBScrollPane(remoteFileList);

    // 本地路径记录
    protected final FixedList<String> localHistory;
    // 远程路径记录
    protected final FixedList<String> remoteHistory;

    public XFTPExplorerUI(Project project, ToolWindow toolWindow) {
        super(project, toolWindow);
        this.initUI();

        PropertiesComponent component = PropertiesComponent.getInstance();
        // 读取历史记录
        this.localHistory = new FixedList<>(HISTORY_MAX_COUNT, component.getValues(LOCAL_HISTORY_PERSISTENCE_KEY));
        this.remoteHistory = new FixedList<>(HISTORY_MAX_COUNT, component.getValues(REMOTE_HISTORY_PERSISTENCE_KEY));
    }

    @Override
    public void onClosed(ContentManagerEvent e) {
        super.onClosed(e);

        // 保存记录
        this.persistHistory();
    }

    /**
     * 初始化UI样式
     */
    protected void initUI() {
        this.splitter.setFirstComponent(this.localWrapper);
        this.splitter.setSecondComponent(this.remoteWrapper);
        this.panelWrapper.add(this.splitter, BorderLayout.CENTER);

        GridBagConstraints noWeight = (GridBagConstraints) Grids.X0Y0.clone();
        noWeight.weightx = 0;
        noWeight.weighty = 0;

        this.localPath.setEditable(true);
        this.localWrapper.add(this.localPath, noWeight);
        this.localWrapper.add(this.localFileListWrapper, Grids.X0Y1);

        this.localFileListWrapper.setBorder(null);

        this.remoteWrapper.add(this.remoteTopWrapper, noWeight);
        this.remoteWrapper.add(this.remoteFileListWrapper, Grids.X0Y1);

        this.remotePath.setEditable(true);
        this.remoteTopWrapper.add(this.remotePath, Grids.X0Y0);
        GridBagConstraints exploreButtonGrid = (GridBagConstraints) Grids.X1Y0.clone();
        exploreButtonGrid.weightx = 0;
        this.remoteTopWrapper.add(this.exploreButton, exploreButtonGrid);
        GridBagConstraints disconnectButtonGrid = (GridBagConstraints) Grids.X2Y0.clone();
        disconnectButtonGrid.weightx = 0;
        this.remoteTopWrapper.add(this.disconnectButton, disconnectButtonGrid);
        GridBagConstraints ttyButtonGrid = (GridBagConstraints) Grids.X3Y0.clone();
        ttyButtonGrid.weightx = 0;
        this.remoteTopWrapper.add(this.newTerminalSessionButton, ttyButtonGrid);

        this.remoteFileListWrapper.setBorder(null);
    }

    /**
     * 获取当前window的UI内容
     */
    public JComponent getUI() {
        return this.panelWrapper;
    }

    /**
     * 设置当前远程内容锁定
     */
    protected void lockRemoteUIs () {
        this.application.invokeLater(() -> this.remoteWrapper.setEnabled(false));
    }

    /**
     * 设置当前远程内容锁定
     */
    protected void unlockRemoteUIs () {
        this.application.invokeLater(() -> this.remoteWrapper.setEnabled(true));
    }

    /**
     * 重新渲染本地路径下拉
     */
    protected void rerenderLocalPath() {
        this.localPath.removeAllItems();
        for (String item : this.localHistory.reversed()) {
            this.localPath.addItem(item);
        }
    }

    /**
     * 重新渲染远程路径下拉
     */
    protected void rerenderRemotePath() {
        this.remotePath.removeAllItems();
        for (String item : this.remoteHistory.reversed()) {
            this.remotePath.addItem(item);
        }
    }

    /**
     * 保存历史浏览记录
     */
    protected void persistHistory() {
        PropertiesComponent component = PropertiesComponent.getInstance();
        component.setValues(LOCAL_HISTORY_PERSISTENCE_KEY, this.localHistory.reversed().toArray(new String[0]));
        component.setValues(REMOTE_HISTORY_PERSISTENCE_KEY, this.remoteHistory.reversed().toArray(new String[0]));
    }

    /**
     * 排序文件
     * @param files 需要排序的文件
     */
    static protected java.util.List<FileModel> sortFiles (java.util.List<FileModel> files) {
        // 将文件根据文件夹->文件、文件名称排序
        files.sort(Comparator.comparing(FileModel::getName));
        java.util.List<FileModel> filesOnly = new ArrayList<>(files.size());
        java.util.List<FileModel> foldersOnly = new ArrayList<>(files.size());
        for (FileModel model : files) {
            if (model.isDirectory()) {
                foldersOnly.add(model);
            } else {
                filesOnly.add(model);
            }
        }
        java.util.List<FileModel> sortedList = new ArrayList<>(files.size());
        sortedList.addAll(foldersOnly);
        sortedList.addAll(filesOnly);

        return sortedList;
    }

    /**
     * 将文件内容放入TableUI
     * @param ui 使用的UI
     * @param files 展示的文件列表
     */
    static protected void rerenderFileTable (FileTable ui, List<FileModel> files) {
        ui.getModel().resetData(sortFiles(files));
    }

}
