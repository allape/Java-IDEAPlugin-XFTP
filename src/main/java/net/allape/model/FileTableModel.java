package net.allape.model;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IconUtil;
import net.allape.util.LinuxPermissions;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.math.BigInteger;
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
