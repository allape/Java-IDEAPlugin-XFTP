package net.allape.models;

import net.allape.utils.LinuxPermissions;
import org.apache.commons.io.FileUtils;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class FileTableModel extends AbstractTableModel {

    static final private String[] COL_NAMES = new String[]{
            "",
            "name",
            "size",
            "permissions",
    };

    private List<FileModel> data;

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
            switch (columnIndex) {
                case 0: return model.getFolder() ? "📁" : "📃";
                case 1: return model.getName();
                case 2: return model.getFolder() || model.getSize() == null ?
                        "" : FileUtils.byteCountToDisplaySize(model.getSize());
                case 3: return model.getPermissions() == null ? "" : LinuxPermissions.humanReadable(model.getPermissions());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public FileTableModel() {
        this(new ArrayList<>());
    }

    public FileTableModel(List<FileModel> data) {
        this.data = data;
    }

    /**
     * 重新设置列表数据
     * @param files 重置至的文件列表
     */
    public void resetData (List<FileModel> files) {
        this.data = files;
        fireTableDataChanged();
    }

    public List<FileModel> getData() {
        return data;
    }

}
