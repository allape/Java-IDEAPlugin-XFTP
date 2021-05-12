package net.allape.window.explorer;

import com.intellij.execution.ui.BaseContentCloseListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.ui.content.Content;
import net.allape.bus.Windows;
import org.jetbrains.annotations.NotNull;

public class XFTPExplorerWindowTabCloseListener extends BaseContentCloseListener {

    private final Project project;
    private final Content content;

    public XFTPExplorerWindowTabCloseListener(@NotNull Content content, @NotNull Project project, @NotNull Disposable parentDisposable) {
        super(content, project, parentDisposable);

        this.project = project;
        this.content = content;
    }

    @Override
    protected void disposeContent(@NotNull Content content) { }

    @Override
    protected boolean closeQuery(@NotNull Content content, boolean projectClosing) {
        if (projectClosing) {
            return true;
        }
        XFTPExplorerWindow window = Windows.windows.get(content);
        if (window != null && window.getSftpClient() != null) {
            return MessageDialogBuilder
                    .yesNo("A server is connected", "Do you really want to close this tab?")
                    .asWarning()
                    .yesText("Close")
                    .ask(this.project);
        }
        return true;
    }

    @Override
    public boolean canClose(@NotNull Project project) {
        return project == this.project && this.closeQuery(this.content, true);
    }
}
