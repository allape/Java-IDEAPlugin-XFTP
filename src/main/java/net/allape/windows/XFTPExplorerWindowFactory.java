package net.allape.windows;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.*;
import net.allape.bus.Data;
import net.allape.bus.Services;
import org.jetbrains.annotations.NotNull;

public class XFTPExplorerWindowFactory implements ToolWindowFactory {

    public static final Logger logger = Logger.getInstance(XFTPExplorerWindowFactory.class);

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        Services.TOOL_WINDOW = toolWindow;
        toolWindow.addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
                XFTPWindow window = Data.windows.get(event.getContent());
                if (window == null) {
                    logger.warn("closed an un-cached window: " + event.toString());
                } else {
                    window.onClosed(event);
                    Data.windows.remove(event.getContent());
                }

                if (toolWindow.getContentManager().getContents().length == 0) {
                    toolWindow.hide();
                }
            }
        });
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        createTheToolWindowContent(project, toolWindow);
    }

    public static void createTheToolWindowContent(Project project, @NotNull ToolWindow toolWindow) {
        // window实例
        XFTPExplorerWindow window = new XFTPExplorerWindow(project, toolWindow);
        //获取内容工厂的实例
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        //获取用于toolWindow显示的内容
        Content content = contentFactory.createContent(window.getPanel(), "Explorer", false);
        content.setCloseable(true);
        //给toolWindow设置内容
        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);

        // 放入缓存
        Data.windows.put(content, window);
    }

}
