package net.allape.window.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.ContentManagerEvent;
import icons.TerminalIcons;
import net.allape.component.FileTable;
import net.allape.component.MemoComboBox;
import net.allape.util.Grids;
import net.allape.window.XFTPWindow;

import javax.swing.*;
import java.awt.*;

public class XFTPExplorerUI extends XFTPWindow {

    public static final String LOCAL_HISTORY_PERSISTENCE_KEY = "xftp.persistence.local-history";
    public static final String REMOTE_HISTORY_PERSISTENCE_KEY = "xftp.persistence.remote-history";

    protected JPanel panelWrapper = new JPanel(new BorderLayout());
    protected JBSplitter splitter = new OnePixelSplitter("xftp-main-window", .5f);

    protected JPanel localWrapper = new JPanel(new GridBagLayout());
    protected JPanel localPathWrapper = new JPanel(new GridBagLayout());
    protected MemoComboBox<String> localPath = new MemoComboBox<>(LOCAL_HISTORY_PERSISTENCE_KEY);
    protected JPanel localActionsPanel = new JPanel();
    protected FileTable localFileList = new FileTable();
    protected JBScrollPane localFileListWrapper = new JBScrollPane(localFileList);

    protected JPanel remoteWrapper = new JPanel(new GridBagLayout());
    protected JPanel remotePathWrapper = new JPanel(new GridBagLayout());
    protected MemoComboBox<String> remotePath = new MemoComboBox<>(REMOTE_HISTORY_PERSISTENCE_KEY);
    protected JButton exploreButton = new JButton(AllIcons.Webreferences.Server);
    protected JButton disconnectButton = new JButton(AllIcons.Actions.Suspend);
    protected JButton newTerminalSessionButton = new JButton(TerminalIcons.OpenTerminal_13x13);
    protected FileTable remoteFileList = new FileTable();
    protected JBScrollPane remoteFileListWrapper = new JBScrollPane(remoteFileList);

    public XFTPExplorerUI(Project project, ToolWindow toolWindow) {
        super(project, toolWindow);
        this.initUI();
    }

    @Override
    public void onClosed(ContentManagerEvent e) {
        super.onClosed(e);
    }

    /**
     * 初始化UI样式
     */
    protected void initUI() {
        final GridBagConstraints noWeight = (GridBagConstraints) Grids.X0Y0.clone();
        noWeight.weightx = 0;
        noWeight.weighty = 0;

        this.localWrapper.setBorder(null);
        this.remoteWrapper.setBorder(null);

        this.splitter.setFirstComponent(this.localWrapper);
        this.splitter.setSecondComponent(this.remoteWrapper);
        this.panelWrapper.add(this.splitter, BorderLayout.CENTER);
        this.panelWrapper.setBorder(null);

        this.localPathWrapper.add(this.localPath, Grids.X0Y0);
        this.localPathWrapper.add(this.localActionsPanel,Grids.X1Y0);
        this.localWrapper.add(this.localPathWrapper, noWeight);

        this.localFileListWrapper.setBorder(null);
        this.localWrapper.add(this.localFileListWrapper, Grids.X0Y1);

        this.remotePathWrapper.add(this.remotePath, Grids.X0Y0);
        GridBagConstraints exploreButtonGrid = (GridBagConstraints) Grids.X1Y0.clone();
        exploreButtonGrid.weightx = 0;
        this.remotePathWrapper.add(this.exploreButton, exploreButtonGrid);
        GridBagConstraints disconnectButtonGrid = (GridBagConstraints) Grids.X2Y0.clone();
        disconnectButtonGrid.weightx = 0;
        this.remotePathWrapper.add(this.disconnectButton, disconnectButtonGrid);
        GridBagConstraints ttyButtonGrid = (GridBagConstraints) Grids.X3Y0.clone();
        ttyButtonGrid.weightx = 0;
        this.remotePathWrapper.add(this.newTerminalSessionButton, ttyButtonGrid);
        this.remoteWrapper.add(this.remotePathWrapper, noWeight);

        this.remoteFileListWrapper.setBorder(null);
        this.remoteWrapper.add(this.remoteFileListWrapper, Grids.X0Y1);
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

}
