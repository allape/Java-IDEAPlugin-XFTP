package net.allape.component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.ComboBox;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Vector;

public class MemoComboBox<E> extends ComboBox<MemoComboBox.MemoComboBoxPersistenceModel<E>> {

    public static final int DEFAULT_MAX_COUNT = 10;

    /**
     * 持久化key
     */
    private final String persistenceKey;

    /**
     * 最大历史记录数量
     */
    private final int maxCount;

    /**
     * 数据字段
     */
    private Field dataField;

    /**
     * @param persistenceKey 保存历史记录的key
     * @param maxCount 最大历史记录数量
     */
    public MemoComboBox(String persistenceKey, int maxCount) {
        super();
        this.persistenceKey = persistenceKey;
        this.maxCount = maxCount;

        this.setRenderer(new ComboBoxCellRenderer());

        // 读取持久化数据
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        try {
            String persistedJson = propertiesComponent.getValue(this.persistenceKey);
            if (persistedJson != null && !persistedJson.isEmpty()) {
                Type listType = new TypeToken<List<MemoComboBoxPersistenceModel<E>>>(){}.getType();
                ((DefaultComboBoxModel<MemoComboBoxPersistenceModel<E>>) this.dataModel)
                        .addAll(new Gson().fromJson(persistedJson, listType));
            }
        } catch (Exception e) {
            e.printStackTrace();
            propertiesComponent.unsetValue(this.persistenceKey);
        }
    }

    /**
     * @param persistenceKey 保存历史记录的key
     */
    public MemoComboBox(String persistenceKey) {
        this(persistenceKey, DEFAULT_MAX_COUNT);
    }

    /**
     * 添加记录
     * @param value 记录的值
     */
    public void push(E value) {
        try {
            if (this.dataField == null) {
                    this.dataField = DefaultComboBoxModel.class.getDeclaredField("objects");
                    this.dataField.setAccessible(true);
            }
            if (this.dataModel instanceof DefaultComboBoxModel) {
                @SuppressWarnings("unchecked")
                Vector<MemoComboBoxPersistenceModel<E>> vector = (Vector<MemoComboBoxPersistenceModel<E>>) this.dataField.get(this.dataModel);
                vector.removeIf(i -> i != null && i.getMemo() != null && i.getMemo().equals(value));
                if (vector.size() > this.maxCount) {
                    vector.removeAll(vector.subList(this.maxCount - 1, vector.size()));
                }

                DefaultComboBoxModel<MemoComboBoxPersistenceModel<E>> model =
                        (DefaultComboBoxModel<MemoComboBoxPersistenceModel<E>>) this.dataModel;
                MemoComboBoxPersistenceModel<E> newMemo = new MemoComboBoxPersistenceModel<>(value);
                model.insertElementAt(newMemo, 0);
                model.setSelectedItem(newMemo);

                // 持久化
                PropertiesComponent.getInstance().setValue(this.persistenceKey, new Gson().toJson(vector));
            } else {
                throw new ClassCastException("Only support for DefaultComboBoxModel");
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public MemoComboBoxPersistenceModel<E> getItem() {
        Object item = this.getSelectedItem();
        if (item instanceof String) {
            return new MemoComboBoxPersistenceModel<>((E)item);
        }
        return super.getItem();
    }

    /**
     * 获取当前选中的值
     * @param defaultValue 默认值
     * @return 当前选中的值
     */
    public E getMemoItem(@Nullable E defaultValue) {
        MemoComboBoxPersistenceModel<E> item = this.getItem();
        if (item == null || item.getMemo() == null) {
            return defaultValue;
        }
        return item.getMemo();
    }

    /**
     * {@link this#getMemoItem(Object)}
     */
    public E getMemoItem() {
        return this.getMemoItem(null);
    }

    public static class ComboBoxCellRenderer extends BasicComboBoxRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }

    }

    @Data
    @NoArgsConstructor
    public static class MemoComboBoxPersistenceModel<E> {

        /**
         * 记录的值
         */
        private E memo;

        /**
         * 是否被标记为喜爱
         */
        private boolean favorite;

        public MemoComboBoxPersistenceModel(E memo) {
            this.memo = memo;
        }

        @Override
        public String toString() {
            return this.memo != null ? this.memo.toString() : "";
        }

    }

}
