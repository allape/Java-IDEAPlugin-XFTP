package net.allape.util

class Maps {

    companion object {

        /**
         * 根据value获取map的第一个匹配的key
         * @param map Map对象
         * @param value 搜索的value
         */
        fun <K, V> getFirstKeyByValue(map: Map<K, V>, value: V?): K? {
            for ((key, value1) in map) {
                if (value is String && value == value1) {
                    return key
                } else if (value === value1) {
                    return key
                }
            }
            return null
        }

    }
}