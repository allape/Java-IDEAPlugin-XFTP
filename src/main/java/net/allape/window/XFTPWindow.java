package net.allape.window;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ssh.RemoteFileObject;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import java.awt.datatransfer.DataFlavor;

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

    // 远程文件拖拽flavor
    protected static final DataFlavor remoteFileListFlavor = new DataFlavor(RemoteFileObject.class, "SSH remote file list");

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

}
