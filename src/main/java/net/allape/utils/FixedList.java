package net.allape.utils;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TODO 等待优化
 */
public class FixedList<T> {

    private final int capacity;

    private final List<T> list;

    public FixedList(int initialCapacity) {
        this(initialCapacity, null);
    }

    public FixedList(int initialCapacity, @Nullable T[] items) {
        this.capacity = initialCapacity;
        this.list = new ArrayList<>(initialCapacity);
        if (items != null) {
            this.list.addAll(Arrays.asList(items));
        }
    }

    public FixedList<T> add(T item) {
        if (item == null) return this;
        int exists = this.list.indexOf(item);
        if (exists != -1) {
            this.list.remove(exists);
        } else if (this.list.size() >= this.capacity) {
            this.list.remove(0);
        }
        this.list.add(item);
        return this;
    }

    public FixedList<T> remove(int index) {
        this.list.remove(index);
        return this;
    }

    public List<T> reversed() {
        List<T> newList = new ArrayList<>(this.list);
        Collections.reverse(newList);
        return newList;
    }

}
