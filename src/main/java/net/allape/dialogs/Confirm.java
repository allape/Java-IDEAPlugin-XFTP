package net.allape.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.concurrent.Future;

public class Confirm extends DialogWrapper {

    final private ConfirmOptions options;

    public Confirm () {
        this(new ConfirmOptions());
    }

    public Confirm (ConfirmOptions options) {
        super(true);

        this.options = options;
        this.setTitle(this.options.getTitle());

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

    @Override
    protected JComponent createSouthPanel() {
        JPanel southPanel = new JPanel(new FlowLayout());

        JButton ok = new JButton(this.options.getOkText());
        ok.addActionListener(this.options.getOnOk());

        JButton cancel = new JButton(this.options.getCancelText());
        ok.addActionListener(this.options.getOnCancel());

        southPanel.add(ok);
        southPanel.add(cancel);
        return southPanel;
    }

    public static final class ConfirmOptions {

        private String title = "Confirming";

        private String content = "Are you sure about this?";

        private String okText = "OK";

        private String cancelText = "Cancel";

        private ActionListener onOk = e -> {};

        private ActionListener onCancel = e -> {};

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

        public ConfirmOptions cancelText(String cancelText) {
            this.cancelText = cancelText;
            return this;
        }

        public ActionListener getOnOk() {
            return onOk;
        }

        public ConfirmOptions onOk(ActionListener onOk) {
            this.onOk = onOk;
            return this;
        }

        public ActionListener getOnCancel() {
            return onCancel;
        }

        public ConfirmOptions onCancel(ActionListener onCancel) {
            this.onCancel = onCancel;
            return this;
        }

    }

}
