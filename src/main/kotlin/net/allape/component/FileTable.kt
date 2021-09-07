package net.allape.component

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.DarculaColors
import com.intellij.ui.JBColor
import com.intellij.ui.table.JBTable
import com.intellij.util.IconUtil
import net.allape.model.FileModel
import net.allape.util.LinuxPermissions
import java.awt.Component
import java.io.File
import java.math.BigInteger
import java.util.*
import javax.swing.Icon
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class TableCellRenderer : DefaultTableCellRenderer() {

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        border = noFocusBorder
        if (value is Icon) {
            text = null
            this.icon = value
            horizontalAlignment = CENTER
        }
        return this
    }
    
}

class FileTableModel(
    var data: List<FileModel> = emptyList()
) : AbstractTableModel() {
    
    companion object {
        private val COL_NAMES = arrayOf(
            "",
            "name",
            "size",
            "permissions"
        )

        private val ONE_KB_BI: BigInteger = BigInteger.valueOf(1024L)
        private val ONE_MB_BI: BigInteger = ONE_KB_BI.multiply(ONE_KB_BI)
        private val ONE_GB_BI: BigInteger = ONE_KB_BI.multiply(ONE_MB_BI)
        private val ONE_TB_BI: BigInteger = ONE_KB_BI.multiply(ONE_GB_BI)
        private val ONE_PB_BI: BigInteger = ONE_KB_BI.multiply(ONE_TB_BI)
        private val ONE_EB_BI: BigInteger = ONE_KB_BI.multiply(ONE_PB_BI)

        /**
         * see [org.apache.commons.io.FileUtils.byteCountToDisplaySize]
         */
        fun byteCountToDisplaySize(longSize: Long): String {
            val size = BigInteger.valueOf(longSize)
            val displaySize: String = if (size.divide(ONE_EB_BI) > BigInteger.ZERO) {
                size.divide(ONE_EB_BI).toString() + " E"
            } else if (size.divide(ONE_PB_BI) > BigInteger.ZERO) {
                size.divide(ONE_PB_BI).toString() + " P"
            } else if (size.divide(ONE_TB_BI) > BigInteger.ZERO) {
                size.divide(ONE_TB_BI).toString() + " T"
            } else if (size.divide(ONE_GB_BI) > BigInteger.ZERO) {
                size.divide(ONE_GB_BI).toString() + " G"
            } else if (size.divide(ONE_MB_BI) > BigInteger.ZERO) {
                size.divide(ONE_MB_BI).toString() + " M"
            } else if (size.divide(ONE_KB_BI) > BigInteger.ZERO) {
                size.divide(ONE_KB_BI).toString() + " K"
            } else {
                "$size b"
            }
            return displaySize
        }
    }

    override fun getRowCount(): Int {
        return data.size
    }

    override fun getColumnCount(): Int {
        return COL_NAMES.size
    }

    override fun getColumnName(column: Int): String {
        return COL_NAMES[column]
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        try {
            val model = data[rowIndex]
            var icon = IconUtil.getEmptyIcon(false)
            if (model.local) {
                val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(model.path))
                if (virtualFile != null) {
                    icon = IconUtil.computeBaseFileIcon(virtualFile)
                }
            }
            when (columnIndex) {
                0 -> return icon
                1 -> return model.name
                2 -> return if (model.directory) "" else byteCountToDisplaySize(model.size)
                3 -> return LinuxPermissions.humanReadable(model.permissions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 重新设置列表数据
     * @param files 重置至的文件列表
     */
    fun resetData(files: List<FileModel>) {
        data = files
        fireTableDataChanged()
    }

}

class FileTable: JBTable(FileTableModel()) {
    
    companion object {
        /**
         * 排序文件
         * @param files 需要排序的文件
         */
        fun sortFiles(files: List<FileModel>): List<FileModel> {
            // 将文件根据文件夹->文件、文件名称排序
            Collections.sort(files, Comparator.comparing(FileModel::name))
            val filesOnly: MutableList<FileModel> = ArrayList(files.size)
            val foldersOnly: MutableList<FileModel> = ArrayList(files.size)
            for (model in files) {
                if (model.directory) {
                    foldersOnly.add(model)
                } else {
                    filesOnly.add(model)
                }
            }
            val sortedList: MutableList<FileModel> = ArrayList(files.size)
            sortedList.addAll(foldersOnly)
            sortedList.addAll(filesOnly)
            return sortedList
        }
    }
    
    init {
        setSelectionBackground(
            JBColor.namedColor(
                "Plugins.lightSelectionBackground",
                DarculaColors.BLUE
            )
        )
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        autoCreateRowSorter = false
        border = null

        val normalRender: DefaultTableCellRenderer = TableCellRenderer()
        val allColumns = getColumnModel().columns
        while (allColumns.hasMoreElements()) {
            allColumns.nextElement().cellRenderer = normalRender
        }

        val iconCell: DefaultTableCellRenderer = TableCellRenderer()
        iconCell.horizontalAlignment = SwingConstants.CENTER
        val typeColumn = getColumnModel().getColumn(0)
        typeColumn.cellRenderer = iconCell
        typeColumn.maxWidth = 30

        val sizeCell: DefaultTableCellRenderer = TableCellRenderer()
        sizeCell.horizontalAlignment = SwingConstants.RIGHT
        val sizeColumn = getColumnModel().getColumn(2)
        sizeColumn.cellRenderer = sizeCell

        val permissionCell: DefaultTableCellRenderer = TableCellRenderer()
        permissionCell.horizontalAlignment = SwingConstants.CENTER
        val permissionsColumn = getColumnModel().getColumn(3)
        permissionsColumn.cellRenderer = permissionCell
    }

    /**
     * 更新table内容
     * @param files 更新的文件
     */
    fun resetData(files: List<FileModel>) {
        this.model.resetData(sortFiles(files))
    }

    override fun getModel(): FileTableModel {
        return super.getModel() as FileTableModel
    }
    
}