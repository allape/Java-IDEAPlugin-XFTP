package net.allape.component;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DarculaColors;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import com.intellij.util.IconUtil;
import net.allape.model.FileModel;
import net.allape.util.LinuxPermissions;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

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

        DefaultTableCellRenderer iconCell = new TableCellRenderer();
        iconCell.setHorizontalAlignment(SwingConstants.CENTER);
        TableColumn typeColumn = this.getColumnModel().getColumn(0);
        typeColumn.setCellRenderer(iconCell);
        typeColumn.setMaxWidth(30);

        DefaultTableCellRenderer sizeCell = new TableCellRenderer();
        sizeCell.setHorizontalAlignment(SwingConstants.RIGHT);
        TableColumn sizeColumn = this.getColumnModel().getColumn(2);
        sizeColumn.setCellRenderer(sizeCell);

        DefaultTableCellRenderer permissionCell = new TableCellRenderer();
        permissionCell.setHorizontalAlignment(SwingConstants.CENTER);
        TableColumn permissionsColumn = this.getColumnModel().getColumn(3);
        permissionsColumn.setCellRenderer(permissionCell);
    }

    /**
     * 更新table内容
     * @param files 更新的文件
     */
    public void resetData(List<FileModel> files) {
        this.getModel().resetData(FileTable.sortFiles(files));
    }

    @Override
    public FileTableModel getModel() {
        return (FileTableModel) super.getModel();
    }

    /**
     * 排序文件
     * @param files 需要排序的文件
     */
    public static List<FileModel> sortFiles(List<FileModel> files) {
        // 将文件根据文件夹->文件、文件名称排序
        files.sort(Comparator.comparing(FileModel::getName));
        List<FileModel> filesOnly = new ArrayList<>(files.size());
        List<FileModel> foldersOnly = new ArrayList<>(files.size());
        for (FileModel model : files) {
            if (model.isDirectory()) {
                foldersOnly.add(model);
            } else {
                filesOnly.add(model);
            }
        }
        List<FileModel> sortedList = new ArrayList<>(files.size());
        sortedList.addAll(foldersOnly);
        sortedList.addAll(filesOnly);

        return sortedList;
    }

    public static class TableCellRenderer extends DefaultTableCellRenderer {

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

    public static class FileTableModel extends AbstractTableModel {

        static final private String[] COL_NAMES = new String[]{
                "",
                "name",
                "size",
                "permissions",
        };

        private java.util.List<FileModel> data;

        @Override
        public int getRowCount() {
            return this.data.size();
        }

        @Override
        public int getColumnCount() {
            return COL_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COL_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            try {
                FileModel model = this.data.get(rowIndex);
                Icon icon = IconUtil.getEmptyIcon(false);
                if (model.isLocal()) {
                    VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(new File(model.getPath()));
                    if (virtualFile != null) {
                        icon = IconUtil.computeBaseFileIcon(virtualFile);
                    }
                }
                switch (columnIndex) {
                    case 0: return icon;
                    case 1: return model.getName();
                    case 2: return model.isDirectory() ?
                            "" : byteCountToDisplaySize(model.getSize());
                    case 3: return LinuxPermissions.humanReadable(model.getPermissions());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public FileTableModel() {
            this(new ArrayList<>());
        }

        public FileTableModel(java.util.List<FileModel> data) {
            this.data = data;
        }

        /**
         * 重新设置列表数据
         * @param files 重置至的文件列表
         */
        public void resetData (java.util.List<FileModel> files) {
            this.data = files;
            fireTableDataChanged();
        }

        public List<FileModel> getData() {
            return data;
        }

        public static final BigInteger ONE_KB_BI = BigInteger.valueOf(1024L);
        public static final BigInteger ONE_MB_BI = ONE_KB_BI.multiply(ONE_KB_BI);
        public static final BigInteger ONE_GB_BI = ONE_KB_BI.multiply(ONE_MB_BI);
        public static final BigInteger ONE_TB_BI = ONE_KB_BI.multiply(ONE_GB_BI);
        public static final BigInteger ONE_PB_BI = ONE_KB_BI.multiply(ONE_TB_BI);
        public static final BigInteger ONE_EB_BI = ONE_KB_BI.multiply(ONE_PB_BI);

        /**
         * see {@link org.apache.commons.io.FileUtils#byteCountToDisplaySize(BigInteger)}
         */
        public static String byteCountToDisplaySize(long longSize) {
            BigInteger size = BigInteger.valueOf(longSize);
            String displaySize;
            if (size.divide(ONE_EB_BI).compareTo(BigInteger.ZERO) > 0) {
                displaySize = size.divide(ONE_EB_BI) + " E";
            } else if (size.divide(ONE_PB_BI).compareTo(BigInteger.ZERO) > 0) {
                displaySize = size.divide(ONE_PB_BI) + " P";
            } else if (size.divide(ONE_TB_BI).compareTo(BigInteger.ZERO) > 0) {
                displaySize = size.divide(ONE_TB_BI) + " T";
            } else if (size.divide(ONE_GB_BI).compareTo(BigInteger.ZERO) > 0) {
                displaySize = size.divide(ONE_GB_BI) + " G";
            } else if (size.divide(ONE_MB_BI).compareTo(BigInteger.ZERO) > 0) {
                displaySize = size.divide(ONE_MB_BI) + " M";
            } else if (size.divide(ONE_KB_BI).compareTo(BigInteger.ZERO) > 0) {
                displaySize = size.divide(ONE_KB_BI) + " K";
            } else {
                displaySize = size + " b";
            }

            return displaySize;
        }

    }
}
