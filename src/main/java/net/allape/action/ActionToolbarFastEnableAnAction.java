package net.allape.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class ActionToolbarFastEnableAnAction extends AnAction {

    private final ActionToolbarImpl toolbar;

    private boolean enabled = true;

    public ActionToolbarFastEnableAnAction(
            ActionToolbarImpl toolbar,
            @Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description,
            @Nullable Icon icon
    ) {
        super(text, description, icon);

        this.toolbar = toolbar;
    }

    public void setEnabled(boolean enabled) {
        this.setEnabled(enabled, false);
    }

    public void setEnabled(boolean enabled, boolean updateUI) {
        this.enabled = enabled;
        if (updateUI) {
            this.toolbar.updateUI();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(this.enabled);
    }
}
