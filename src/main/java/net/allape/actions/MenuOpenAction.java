package net.allape.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogWrapper;
import net.allape.bus.Services;
import net.allape.dialogs.Confirm;
import net.allape.windows.XFTPExplorerWindowFactory;
import org.jetbrains.annotations.NotNull;

public class MenuOpenAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (Services.TOOL_WINDOW != null) {
            XFTPExplorerWindowFactory.createTheToolWindowContent(e.getProject(), Services.TOOL_WINDOW);
        } else {
            DialogWrapper notInitYet = new Confirm(
                    new Confirm.ConfirmOptions()
                            .title("Initialing...")
                            .content("IPlease wait for a while.😊")
            );
            notInitYet.setOKActionEnabled(false);
            notInitYet.show();
        }
    }
}
