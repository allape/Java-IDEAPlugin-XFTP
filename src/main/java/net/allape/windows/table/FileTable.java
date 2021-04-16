package net.allape.windows.table;

import com.intellij.ui.DarculaColors;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import net.allape.models.FileTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.Enumeration;

public class FileTable extends JBTable {

    public FileTable() {
        super(new FileTableModel());
        
        this.setSelectionBackground(JBColor.namedColor(
                "Plugins.lightSelectionBackground",
                DarculaColors.BLUE
        ));
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setAutoCreateRowSorter(false);
        this.setBorder(null);

        DefaultTableCellRenderer normalRender = new TableCellRenderer();
        Enumeration<TableColumn> allColumns = this.getColumnModel().getColumns();
        while (allColumns.hasMoreElements()) {
            allColumns.nextElement().setCellRenderer(normalRender);
        }

        DefaultTableCellRenderer centerRenderer = new TableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightRenderer = new TableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        TableColumn typeColumn = this.getColumnModel().getColumn(0);
        typeColumn.setCellRenderer(centerRenderer);
        typeColumn.setMaxWidth(30);

        TableColumn sizeColumn = this.getColumnModel().getColumn(2);
        sizeColumn.setCellRenderer(rightRenderer);

        TableColumn permissionsColumn = this.getColumnModel().getColumn(3);
        permissionsColumn.setCellRenderer(centerRenderer);
    }

    @Override
    public FileTableModel getModel() {
        return (FileTableModel) super.getModel();
    }

    static class TableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(noFocusBorder);
            if (value instanceof Icon) {
                this.setText(null);
                this.setIcon((Icon) value);
                this.setHorizontalAlignment(JLabel.CENTER);
            }
            return this;
        }

    }
}
