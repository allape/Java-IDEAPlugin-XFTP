package net.allape.windows.explorer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.DarculaColors;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import net.allape.models.FileModel;
import net.allape.models.FileTableModel;
import net.allape.utils.Grids;
import net.allape.windows.XFTPWindow;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

public class XFTPExplorerUI extends XFTPWindow {

    protected JBLayeredPane panelWrapper = new JBLayeredPane();
    protected JPanel panel = new JPanel(new GridLayout());

    @SuppressWarnings("rawtypes")
    protected JBPanel localWrapper = new JBPanel(new GridLayout());
    protected JBScrollPane localFileListWrapper = new JBScrollPane();
    protected JBTextField localPath = new JBTextField();
    protected JBList<FileModel> localFileList = new JBList<>(new DefaultListModel<>());

    @SuppressWarnings("rawtypes")
    protected JBPanel remoteWrapper = new JBPanel(new GridLayout());
    @SuppressWarnings("rawtypes")
    protected JBPanel remoteTopWrapper = new JBPanel(new GridLayout());
    protected JBTextField remotePath = new JBTextField();
    protected JButton exploreButton = new JButton("Explorer");
    protected JButton disconnectButton = new JButton("Disconnect");
    protected JBScrollPane remoteFileListWrapper = new JBScrollPane();
    protected JBTable remoteFileList = new JBTable(new FileTableModel());

    public XFTPExplorerUI(Project project, ToolWindow toolWindow) {
        super(project, toolWindow);
        this.initUI();
    }

    /**
     * 初始化UI样式
     */
    protected void initUI() {
        this.panelWrapper.add(this.panel, 2, 0);

        this.panel.add(this.localWrapper, Grids.X0Y0);
        this.panel.add(this.remoteWrapper, Grids.X1Y0);

        this.localWrapper.add(this.localPath, Grids.X0Y0);
        this.localWrapper.add(this.localFileListWrapper, Grids.X0Y1);

        this.localFileListWrapper.add(this.localFileList);
        this.localFileListWrapper.setBorder(null);

        this.localFileList.setCellRenderer(new XFTPExplorerWindowListCellRenderer());
        this.localFileList.setSelectionBackground(JBColor.namedColor(
                "Plugins.lightSelectionBackground",
                DarculaColors.BLUE
        ));
        this.localFileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        this.remoteWrapper.add(this.remoteTopWrapper, Grids.X0Y0);
        this.remoteWrapper.add(this.remoteFileListWrapper, Grids.X0Y1);

        this.remoteTopWrapper.add(this.remotePath, Grids.X0Y0);
        this.remoteTopWrapper.add(this.exploreButton, Grids.X1Y0);
        this.remoteTopWrapper.add(this.disconnectButton, Grids.X2Y0);

        this.remoteFileListWrapper.add(this.remoteFileList);
        this.remoteFileListWrapper.setBorder(null);

        this.remoteFileList.setSelectionBackground(JBColor.namedColor(
                "Plugins.lightSelectionBackground",
                DarculaColors.BLUE
        ));
        this.remoteFileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.remoteFileList.setAutoCreateRowSorter(false);
        this.remoteFileList.setBorder(null);
        TableColumn typeColumn = this.remoteFileList.getColumnModel().getColumn(0);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        typeColumn.setCellRenderer(centerRenderer);
        typeColumn.setMaxWidth(25);
    }

    /**
     * 获取JPanel
     * @return JPanel
     */
    public JLayeredPane getPanelWrapper() {
        return this.panelWrapper;
    }

}
