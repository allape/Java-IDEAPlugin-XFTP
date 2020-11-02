package net.allape.windows;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class XFTPExplorerWindowListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        this.setBorder(JBUI.Borders.empty(3, 5, 3, 0));
        this.setForeground(JBColor.foreground());
        return this;
    }
}
