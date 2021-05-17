package net.allape.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class ActionToolbarFastEnableAnAction extends DumbAwareAction {

    private final ActionToolbarImpl toolbar;

    private boolean enabled = true;
    private Icon icon;

    public ActionToolbarFastEnableAnAction(
            ActionToolbarImpl toolbar,
            @Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description,
            @Nullable Icon icon
    ) {
        super(text, description, icon);

        this.toolbar = toolbar;
        this.icon = icon;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(this.enabled);
        e.getPresentation().setIcon(this.icon);
    }
}
