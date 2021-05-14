package net.allape.window.explorer;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import net.allape.component.FileTable;
import net.allape.component.MemoComboBox;
import net.allape.util.Grids;
import net.allape.window.XFTPWindow;

import javax.swing.*;
import java.awt.*;

public class XFTPExplorerUI extends XFTPWindow {

    public static final String LOCAL_HISTORY_PERSISTENCE_KEY = "xftp.persistence.local-history";
    public static final String REMOTE_HISTORY_PERSISTENCE_KEY = "xftp.persistence.remote-history";

    protected static final String LOCAL_TOOL_BAR_PLACE = "XFTPLocalToolBar";
    protected static final String REMOTE_TOOL_BAR_PLACE = "XFTPRemoteToolBar";

    protected JPanel panelWrapper = new JPanel(new BorderLayout());
    protected JBSplitter splitter = new OnePixelSplitter("xftp-main-window", .5f);

    protected JPanel localWrapper = new JPanel(new GridBagLayout());
    protected JPanel localPathWrapper = new JPanel(new GridBagLayout());
    protected MemoComboBox<String> localPath = new MemoComboBox<>(LOCAL_HISTORY_PERSISTENCE_KEY);
    protected FileTable localFileList = new FileTable();
    protected JBScrollPane localFileListWrapper = new JBScrollPane(localFileList);
    protected DefaultActionGroup localActionGroup = new DefaultActionGroup();
    protected ActionToolbarImpl localActionToolBar = new ActionToolbarImpl(LOCAL_TOOL_BAR_PLACE, localActionGroup, false);

    protected JPanel remoteWrapper = new JPanel(new GridBagLayout());
    protected JPanel remotePathWrapper = new JPanel(new GridBagLayout());
    protected MemoComboBox<String> remotePath = new MemoComboBox<>(REMOTE_HISTORY_PERSISTENCE_KEY);
    protected DefaultActionGroup remoteActionGroup = new DefaultActionGroup();
    protected ActionToolbarImpl remoteActionToolBar = new ActionToolbarImpl(REMOTE_TOOL_BAR_PLACE, remoteActionGroup, false);
    protected FileTable remoteFileList = new FileTable();
    protected JBScrollPane remoteFileListWrapper = new JBScrollPane(remoteFileList);

    public XFTPExplorerUI(Project project, ToolWindow toolWindow) {
        super(project, toolWindow);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    /**
     * 初始化UI样式
     */
    protected void initUI() {
        final GridBagConstraints noYWeightX0Y0 = (GridBagConstraints) Grids.X0Y0.clone();
        noYWeightX0Y0.weighty = 0;

        final GridBagConstraints noXWeightX0Y0 = (GridBagConstraints) Grids.X0Y0.clone();
        noXWeightX0Y0.weightx = 0;

        final GridBagConstraints noXWeightX1Y0 = (GridBagConstraints) Grids.X1Y0.clone();
        noXWeightX1Y0.weightx = 0;

        // region 本地
        this.localPathWrapper.add(this.localPath, noYWeightX0Y0);

        this.localFileListWrapper.setBorder(null);
        this.localPathWrapper.add(this.localFileListWrapper, Grids.X0Y1);
        this.localWrapper.add(this.localPathWrapper, Grids.X0Y0);

        JPanel localActionToolBarWrapper = new JPanel(new BorderLayout());
        localActionToolBarWrapper.setMinimumSize(new Dimension(48, 0));
        localActionToolBarWrapper.add(this.localActionToolBar);
        this.localWrapper.add(localActionToolBarWrapper, noXWeightX1Y0);
        // endregion

        // region 远程
        JPanel remoteActionToolBarWrapper = new JPanel(new BorderLayout());
        remoteActionToolBarWrapper.setMinimumSize(new Dimension(48, 0));
        remoteActionToolBarWrapper.add(this.remoteActionToolBar);
        this.remoteWrapper.add(remoteActionToolBarWrapper, noXWeightX0Y0);

        this.remotePathWrapper.add(this.remotePath, noYWeightX0Y0);

        this.remoteFileListWrapper.setBorder(null);
        this.remotePathWrapper.add(this.remoteFileListWrapper, Grids.X0Y1);

        this.remoteWrapper.add(this.remotePathWrapper, Grids.X1Y0);
        // endregion

        this.localWrapper.setBorder(null);
        this.remoteWrapper.setBorder(null);

        this.splitter.setFirstComponent(this.localWrapper);
        this.splitter.setSecondComponent(this.remoteWrapper);

        this.panelWrapper.add(this.splitter, BorderLayout.CENTER);
        this.panelWrapper.setBorder(null);
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
