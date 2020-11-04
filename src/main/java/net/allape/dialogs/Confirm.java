package net.allape.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class Confirm extends DialogWrapper {

    final private ConfirmOptions options;

    public Confirm () {
        this(new ConfirmOptions());
    }

    public Confirm (ConfirmOptions options) {
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
        // 创建一个面板，设置其布局为边界布局
        JPanel centerPanel = new JPanel(new BorderLayout());
        // 创建一个文字标签，来承载内容
        JLabel label = new JLabel(this.options.getContent());
        // 设置首先大小
        label.setPreferredSize(new Dimension(100, 100));
        // 将文字标签添加的面板的正中间
        centerPanel.add(label, BorderLayout.CENTER);
        return centerPanel;
    }

    public static final class ConfirmOptions {

        private String title = "Confirming";

        private String content = "Are you sure about this?";

        private String okText = "OK";

        private String cancelText = "Cancel";

        public String getTitle() {
            return title;
        }

        public ConfirmOptions title(String title) {
            this.title = title;
            return this;
        }

        public String getContent() {
            return content;
        }

        public ConfirmOptions content(String content) {
            this.content = content;
            return this;
        }

        public String getOkText() {
            return okText;
        }

        public ConfirmOptions okText(String okText) {
            this.okText = okText;
            return this;
        }

        public String getCancelText() {
            return cancelText;
        }

    }

}
