package net.allape.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class ActionToolbarFastEnableAnAction extends AnAction {

    private final ActionToolbarImpl toolbar;

    // 当前状态
    private boolean enabled;

    public ActionToolbarFastEnableAnAction(
            ActionToolbarImpl toolbar,
            @Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description,
            @Nullable Icon icon
    ) {
        super(text, description, icon);

        this.toolbar = toolbar;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.toolbar.getPresentation(this).setEnabled(enabled);
    }

}
