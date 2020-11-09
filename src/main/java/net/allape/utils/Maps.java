package net.allape.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class Maps {

    /**
     * 根据value获取map的第一个匹配的key
     * @param map Map对象
     * @param value 搜索的value
     */
    public static <@Nullable K, @Nullable V> @Nullable K getFirstKeyByValue(@NotNull Map<K, V> map, @Nullable V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (value instanceof String && value.equals(entry.getValue())) {
                return entry.getKey();
            } else if (value == entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }

}
