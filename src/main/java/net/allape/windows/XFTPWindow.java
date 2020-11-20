package net.allape.windows;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import net.allape.models.FileModel;
import net.allape.models.FileTableModel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class XFTPWindow {

    // 服务器文件系统分隔符
    protected static final String SERVER_FILE_SYSTEM_SEPARATOR = "/";

    // 默认集合大小
    protected static final int COLLECTION_SIZE = 100;

    // 当前用户本地home目录
    protected static final String USER_HOME = System.getProperty("user.home");

    // 双击间隔, 毫秒
    protected static final long DOUBLE_CLICK_INTERVAL = 350;

    // 最大可打开文件
    protected static final long EDITABLE_FILE_SIZE = 2 * 1024 * 1024;

    // 双击监听
    protected long clickWatcher = System.currentTimeMillis();

    final protected Project project;
    final protected ToolWindow toolWindow;
    final protected Application application;

    public XFTPWindow (Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.application = ApplicationManager.getApplication();
    }

    /**
     * 页面关闭时
     * @param e {@link ContentManagerListener#contentRemoved(ContentManagerEvent)}的参数
     */
    public void onClosed (ContentManagerEvent e) { }

    /**
     * 排序文件
     * @param files 需要排序的文件
     */
    static protected List<FileModel> sortFiles (List<FileModel> files) {
        // 将文件根据文件夹->文件、文件名称排序
        files.sort(Comparator.comparing(FileModel::getName));
        List<FileModel> filesOnly = new ArrayList<>(files.size());
        List<FileModel> foldersOnly = new ArrayList<>(files.size());
        for (FileModel model : files) {
            if (model.getFolder()) {
                foldersOnly.add(model);
            } else {
                filesOnly.add(model);
            }
        }
        List<FileModel> sortedList = new ArrayList<>(files.size());
        sortedList.addAll(foldersOnly);
        sortedList.addAll(filesOnly);

        return sortedList;
    }

    /**
     * 将文件内容放入ListUI
     * @param ui 使用的UI
     * @param files 展示的文件列表
     */
    static protected void rerenderFileList (JList<FileModel> ui, List<FileModel> files) {
        // 清空列表后将现在的内容添加进去
        ui.clearSelection();
        ui.setListData(sortFiles(files).toArray(new FileModel[]{}));
    }

    /**
     * 将文件内容放入TableUI
     * @param ui 使用的UI
     * @param files 展示的文件列表
     */
    static protected void rerenderFileTable (JTable ui, List<FileModel> files) {
        ((FileTableModel) (ui.getModel())).resetData(sortFiles(files));
    }

}
