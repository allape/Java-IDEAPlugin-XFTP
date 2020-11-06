package net.allape.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class Confirm extends DialogWrapper {

    final private Options options;

    public Confirm (Options options) {
        super(true);

        this.options = options;
        this.setTitle(this.options.getTitle());
        this.setOKButtonText(this.options.getOkText());
        this.setCancelButtonText(this.options.getCancelText());

        this.setButtonsAlignment(SwingConstants.CENTER);
        this.setResizable(false);

        this.init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel centerPanel = new JPanel(new GridBagLayout());

        String[] lines = this.options.getContent().split("\n");
        for (int i = 0; i < lines.length; i++) {
            JLabel label = new JLabel(lines[i], SwingConstants.CENTER);

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = i;
            if (i == 0) {
                constraints.insets = JBUI.insets(10, 10, 0, 10);
            }
            if (i > 0) {
                constraints.insets = JBUI.insets(5, 10, 0, 10);
            }
            if (i == lines.length - 1) {
                constraints.insets = JBUI.insets(5, 10, 10, 10);
            }
            centerPanel.add(label, constraints);
        }

        return centerPanel;
    }

    public static final class Options {

        private String title = "Confirming";

        private String content = "Are you sure about this?";

        private String okText = "OK";

        private String cancelText = "Cancel";

        public String getTitle() {
            return title;
        }

        public Options title(String title) {
            this.title = title;
            return this;
        }

        public String getContent() {
            return content;
        }

        public Options content(String content) {
            this.content = content;
            return this;
        }

        public String getOkText() {
            return okText;
        }

        public Options okText(String okText) {
            this.okText = okText;
            return this;
        }

        public String getCancelText() {
            return cancelText;
        }

        public Options cancelText(String cancelText) {
            this.cancelText = cancelText;
            return this;
        }
    }

}
