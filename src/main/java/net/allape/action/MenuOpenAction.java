package net.allape.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.ui.UIUtil;
import net.allape.bus.Services;
import net.allape.window.XFTPExplorerWindowFactory;
import org.jetbrains.annotations.NotNull;

public class MenuOpenAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ToolWindow toolWindow = Services.TOOL_WINDOW;
        if (toolWindow != null) {
            toolWindow.show(() -> XFTPExplorerWindowFactory.createTheToolWindowContent(e.getProject(), toolWindow));
        } else {
            MessageDialogBuilder
                    .okCancel("Initialing...", "Please wait for a while.")
                    .icon(UIUtil.getInformationIcon())
                    .yesText("Ok")
                    .ask(e.getProject());
        }
    }
}
