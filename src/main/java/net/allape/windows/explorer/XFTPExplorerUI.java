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

    protected JPanel panelWrapper = new JPanel(new BorderLayout());
    protected JPanel panel = new JPanel(new GridBagLayout());

    @SuppressWarnings("rawtypes")
    protected JBPanel localWrapper = new JBPanel(new GridBagLayout());
    protected JBScrollPane localFileListWrapper = new JBScrollPane();
    protected JBTextField localPath = new JBTextField();
    protected JBList<FileModel> localFileList = new JBList<>(new DefaultListModel<>());

    @SuppressWarnings("rawtypes")
    protected JBPanel remoteWrapper = new JBPanel(new GridBagLayout());
    @SuppressWarnings("rawtypes")
    protected JBPanel remoteTopWrapper = new JBPanel(new GridBagLayout());
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
        // 使用反射初始化所有组件
        /*try {
            Class<JComponent> componentClass = JComponent.class;
            Class<XFTPExplorerUI> uiClass = XFTPExplorerUI.class;
            for (Field field : uiClass.getDeclaredFields()) {
                field.setAccessible(true);
                if (componentClass.isAssignableFrom(field.getType()) || field.getType() == componentClass) {
                    this.initSingleUI((JComponent) field.get(this));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Services.message("Error occurred while initialing UI, please re-open a window for retrying..." + e.getMessage(), MessageType.ERROR);
        }*/

        this.panelWrapper.add(this.panel, BorderLayout.CENTER);

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
     * 对单个组件进行常规化操作
     * @param component 组件
     */
    protected void initSingleUI (JComponent component) {
        component.setPreferredSize(new Dimension(100, 100));
    }

    /**
     * 获取当前window的UI内容
     */
    public JComponent getUI() {
        return this.panelWrapper;
    }

}
