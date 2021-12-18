package net.allape.util

class LinuxHelper {

    companion object {

        /**
         * 转换linux文件权限为rwx的格式
         * @param permissions 权限
         * @return 人类可读的权限
         */
        fun humanReadable(permissions: Int): String {
            val numbers = Integer.toOctalString(permissions and 0xfff).split("").toTypedArray()
            val builder = StringBuilder(numbers.size * 3)
            for (number in numbers) {
                when (number) {
                    "0" -> builder.append("---")
                    "1" -> builder.append("--x")
                    "2" -> builder.append("-w-")
                    "3" -> builder.append("-wx")
                    "4" -> builder.append("r--")
                    "5" -> builder.append("r-x")
                    "6" -> builder.append("rw-")
                    "7" -> builder.append("rwx")
                }
            }
            return builder.toString()
        }

        /**
         * 转译shell字符串, 所有单引号被
         */
        fun escapeShellString(string: String): String {
            return "'${string.replace(Regex("'"), "'\\\\''")}'"
        }

    }

}