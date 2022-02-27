package net.allape.xftp.component

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.lang.reflect.Field
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JList
import javax.swing.plaf.basic.BasicComboBoxRenderer
import kotlin.concurrent.schedule

class ComboBoxCellRenderer: BasicComboBoxRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component? {
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    }
}

data class MemoComboBoxPersistenceModel<E>(
    var memo: E?,
    var favorite: Boolean = false,
) {

    override fun toString(): String = memo?.toString() ?: ""

}

class MemoComboBox<E>(
    /**
     * 持久化key
     */
    private val persistenceKey: String,
    /**
     * 最大历史记录数量
     */
    private val maxCount: Int = DEFAULT_MAX_COUNT,
): ComboBox<MemoComboBoxPersistenceModel<E>>() {

    companion object {
        private const val DEFAULT_MAX_COUNT = 10
    }

    /**
     * 当前应用
     */
    private val application: Application = ApplicationManager.getApplication()

    /**
     * 数据字段
     */
    private var dataField: Field? = null

    /**
     * 是否在下一次获取焦点时显示dropdown
     */
    private var popupAfterGainedFocus: Boolean = false

    /**
     * 当前组件是否获取了焦点
     */
    private var focued: Boolean = false

    init {
        setRenderer(ComboBoxCellRenderer())
        setEditable(true)

        // 读取持久化数据
        val propertiesComponent = PropertiesComponent.getInstance()
        try {
            val persistedJson = propertiesComponent.getValue(persistenceKey)
            if (persistedJson != null && persistedJson.isNotEmpty()) {
                val listType = object : TypeToken<List<MemoComboBoxPersistenceModel<E>?>?>() {}.type
                (dataModel as DefaultComboBoxModel<MemoComboBoxPersistenceModel<E>?>)
                    .addAll(Gson().fromJson(persistedJson, listType))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            propertiesComponent.unsetValue(persistenceKey)
        }

        // 初始化focus监听
        addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {
                focued = true
                if (popupAfterGainedFocus) {
                    popupAfterGainedFocus = false
                    // FIXME 因为马上设置为true时, 因为焦点获取延时的问题, 会导致popup马上关闭
                    Timer().schedule(200) {
                        isPopupVisible = true
                    }
                }
            }
            override fun focusLost(e: FocusEvent?) {
                focued = false
            }
        })
    }

    /**
     * 添加记录
     * @param value 记录的值
     */
    @Suppress("UNCHECKED_CAST")
    fun push(value: E) {
        try {
            if (this.dataField == null) {
                this.dataField = DefaultComboBoxModel::class.java.getDeclaredField("objects")
                this.dataField!!.isAccessible = true
            }
            if (dataModel is DefaultComboBoxModel<*>) {
                val vector = this.dataField!!.get(dataModel) as Vector<MemoComboBoxPersistenceModel<E>>
                vector.removeIf { i: MemoComboBoxPersistenceModel<E>? ->
                    i?.memo != null && i.memo!! == value
                }
                if (vector.size > maxCount) {
                    vector.removeAll(vector.subList(maxCount - 1, vector.size).toSet())
                }
                val model = dataModel as DefaultComboBoxModel<MemoComboBoxPersistenceModel<E>>
                val newMemo = MemoComboBoxPersistenceModel(value)
                model.insertElementAt(newMemo, 0)
                application.invokeLater { model.setSelectedItem(newMemo) }

                // 持久化
                application.executeOnPooledThread {
                    try {
                        PropertiesComponent.getInstance()
                            .setValue(persistenceKey, Gson().toJson(vector))
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                throw ClassCastException("Only support for DefaultComboBoxModel")
            }
        } catch (e: ReflectiveOperationException) {
            e.printStackTrace()
        }
    }

    override fun getItem(): MemoComboBoxPersistenceModel<E>? {
        return if (selectedItem is String) {
            @Suppress("UNCHECKED_CAST")
            MemoComboBoxPersistenceModel(selectedItem as E)
        } else super.getItem()
    }

    /**
     * 获取当前选中的值
     * @param defaultValue 默认值
     * @return 当前选中的值
     */
    fun getMemoItem(defaultValue: E? = null): E? = item?.memo ?: defaultValue

    /**
     * 获取焦点并显示下拉框内容
     */
    fun focusAndPopup() {
        if (isPopupVisible) {
            isPopupVisible = false
        } else {
            if (focued) {
                isPopupVisible = true
            } else {
                popupAfterGainedFocus = true
                requestFocus()
            }
        }
    }

}