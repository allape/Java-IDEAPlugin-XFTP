package net.allape.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class FastEnableAnAction extends AnAction {

    protected static final Presentation ENABLED_PRESENTATION = new Presentation();
    static {
        ENABLED_PRESENTATION.setEnabled(true);
    }

    protected static final Presentation DISABLED_PRESENTATION = new Presentation();
    static {
        DISABLED_PRESENTATION.setEnabled(false);
    }

    private String place;

    public FastEnableAnAction(Icon icon) {
        super(icon);
    }

    public FastEnableAnAction(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    public FastEnableAnAction(
            String place,
            @Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon
    ) {
        super(text, description, icon);
        this.place = place;
    }

    /**
     * see {@link this#setEnabled(String, boolean)}
     */
    public void setEnabled(boolean enable) {
        this.setEnabled(this.place == null ? ActionPlaces.UNKNOWN : this.place, enable);
    }

    /**
     * 设置当前行为状态
     * @param place {@link ActionPlaces}
     * @param enable 是否启用
     */
    public void setEnabled(String place, boolean enable) {
        this.update(AnActionEvent.createFromDataContext(
                place,
                enable ? ENABLED_PRESENTATION : DISABLED_PRESENTATION,
                DataContext.EMPTY_CONTEXT
        ));
    }

}
