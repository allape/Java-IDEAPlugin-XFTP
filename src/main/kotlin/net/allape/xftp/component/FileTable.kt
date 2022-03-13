package net.allape.xftp.component

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.table.JBTable
import net.allape.model.FileModel
import net.allape.model.FileModelType
import net.allape.util.LinuxHelper
import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
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

            var icon = AllIcons.FileTypes.Unknown
            if (model.directory) {
                icon = AllIcons.Nodes.Folder
            } else {
                // 获取文件后缀
                FileUtil.getExtension(model.path, "").takeIf { it.isNotEmpty() }?.let {
                    // 根据后缀读取文件图标
                    FileTypeManager.getInstance().getFileTypeByExtension(it.toString()).icon?.let { i ->
                        icon = i
                    }
                }
            }
//            if (model.local) {
//                val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(model.path))
//                if (virtualFile != null) {
//                    icon = IconUtil.computeBaseFileIcon(virtualFile)
//                }
//            }
            when (columnIndex) {
                0 -> return icon
                1 -> return model.name
                2 -> return if (model.directory) "" else byteCountToDisplaySize(model.size)
                3 -> return LinuxHelper.humanReadable(model.permissions)
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

// TODO add fast search
// com.intellij.ui.SpeedSearchBase
// com.intellij.ui.FilteringTree

interface IDoubleClickListener {
    fun onDoubleClick(file: FileModel)
}

class FileTable: JBTable(FileTableModel()) {
    
    companion object {
        // 双击间隔, 毫秒
        const val DOUBLE_CLICK_INTERVAL: Long = 350
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

    var focused: Boolean = false
        private set

    private val doubleClickListeners: MutableList<IDoubleClickListener> = mutableListOf()

    // 双击点击监听
    private var clickWatcher: Long = System.currentTimeMillis()

    private var lastSelectedRow: Int = -1

    init {
//        setSelectionBackground(
//            JBColor.namedColor(
//                "Plugins.lightSelectionBackground",
//                DarculaColors.BLUE
//            )
//        )
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

        addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {
                focused = true
            }
            override fun focusLost(e: FocusEvent?) {
                focused = false
            }
        })

        selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                lastSelectedRow = selectedRow
            }
        }

        addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                if (e?.mouseButton == MouseButton.Left) {
                    if (selectedRow != -1 && lastSelectedRow == selectedRow && doubleClickListeners.size > 0) {
                        val now = System.currentTimeMillis()
                        if (now - clickWatcher < DOUBLE_CLICK_INTERVAL) {
                            doubleClickListeners.forEach { l -> l.onDoubleClick(model.data[selectedRow]) }
                        }
                        clickWatcher = now
                    }
                }
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
    }

    /**
     * 更新table内容
     * @param files 更新的文件
     */
    fun resetData(files: List<FileModel>) {
        this.model.resetData(sortFiles(files))
    }

    /**
     * 获取当前选中了的真实文件
     */
    fun selected(): List<FileModel> {
        val rows = this.selectedRows
        val files: MutableList<FileModel> = ArrayList(rows.size)
        if (rows.isEmpty()) {
            return files
        }
        val fileModels = this.model.data
        for (row in rows) {
            val fileModel = fileModels[row]
            if (fileModel.type == FileModelType.NON_FILE) continue
            files.add(fileModel)
        }
        return files
    }

    /**
     * 添加双击监听器
     */
    fun addDoubleClickListener(doubleClickListener: IDoubleClickListener) {
        doubleClickListeners.add(doubleClickListener)
    }

    override fun getModel(): FileTableModel {
        return super.getModel() as FileTableModel
    }
    
}