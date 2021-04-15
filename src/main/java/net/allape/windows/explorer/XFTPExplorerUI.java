package net.allape.windows.explorer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import net.allape.models.FileModel;
import net.allape.models.FileTableModel;
import net.allape.utils.Grids;
import net.allape.windows.XFTPWindow;
import net.allape.windows.table.FileTable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class XFTPExplorerUI extends XFTPWindow {

    protected JPanel panelWrapper = new JPanel(new BorderLayout());
    protected JBSplitter splitter = new OnePixelSplitter("xftp-main-window", .5f);

    @SuppressWarnings("rawtypes")
    protected JBPanel localWrapper = new JBPanel(new GridBagLayout());
    protected JBTextField localPath = new JBTextField();
    protected FileTable localFileList = new FileTable();
    protected JBScrollPane localFileListWrapper = new JBScrollPane(localFileList);

    @SuppressWarnings("rawtypes")
    protected JBPanel remoteWrapper = new JBPanel(new GridBagLayout());
    @SuppressWarnings("rawtypes")
    protected JBPanel remoteTopWrapper = new JBPanel(new GridBagLayout());
    protected JBTextField remotePath = new JBTextField();
    protected JButton exploreButton = new JButton("Explorer");
    protected JButton disconnectButton = new JButton("Disconnect");
    protected FileTable remoteFileList = new FileTable();
    protected JBScrollPane remoteFileListWrapper = new JBScrollPane(remoteFileList);

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

        this.splitter.setFirstComponent(this.localWrapper);
        this.splitter.setSecondComponent(this.remoteWrapper);
        this.panelWrapper.add(this.splitter, BorderLayout.CENTER);

        GridBagConstraints noWeight = (GridBagConstraints) Grids.X0Y0.clone();
        noWeight.weightx = 0;
        noWeight.weighty = 0;

        this.localWrapper.add(this.localPath, noWeight);
        this.localWrapper.add(this.localFileListWrapper, Grids.X0Y1);

        this.localFileListWrapper.setBorder(null);

        this.remoteWrapper.add(this.remoteTopWrapper, noWeight);
        this.remoteWrapper.add(this.remoteFileListWrapper, Grids.X0Y1);

        this.remoteTopWrapper.add(this.remotePath, Grids.X0Y0);
        GridBagConstraints exploreButtonGrid = (GridBagConstraints) Grids.X1Y0.clone();
        exploreButtonGrid.weightx = 0;
        this.remoteTopWrapper.add(this.exploreButton, exploreButtonGrid);
        GridBagConstraints disconnectButtonGrid = (GridBagConstraints) Grids.X2Y0.clone();
        disconnectButtonGrid.weightx = 0;
        this.remoteTopWrapper.add(this.disconnectButton, disconnectButtonGrid);

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
        this.remoteWrapper.setEnabled(false);
    }

    /**
     * 设置当前远程内容锁定
     */
    protected void unlockRemoteUIs () {
        this.remoteWrapper.setEnabled(true);
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
            if (model.getFolder()) {
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
