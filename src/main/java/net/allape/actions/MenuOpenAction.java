package net.allape.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.ToolWindow;
import net.allape.bus.Services;
import net.allape.dialogs.Confirm;
import net.allape.windows.explorer.XFTPExplorerWindowFactory;
import org.jetbrains.annotations.NotNull;

public class MenuOpenAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ToolWindow toolWindow = Services.TOOL_WINDOW;
        if (toolWindow != null) {
            toolWindow.show(() -> XFTPExplorerWindowFactory.createTheToolWindowContent(e.getProject(), toolWindow));
        } else {
            DialogWrapper notInitYet = new Confirm(
                    new Confirm.Options()
                            .title("Initialing...")
                            .content("IPlease wait for a while.😊")
            );
            notInitYet.setOKActionEnabled(false);
            notInitYet.show();
        }
    }
}
