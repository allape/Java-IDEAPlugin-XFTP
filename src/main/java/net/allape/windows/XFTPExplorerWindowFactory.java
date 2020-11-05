package net.allape.windows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import net.allape.bus.Services;
import org.jetbrains.annotations.NotNull;

public class XFTPExplorerWindowFactory implements ToolWindowFactory {

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        Services.TOOL_WINDOW = toolWindow;
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
        toolWindow.getContentManager().addContent(content);
    }

}
