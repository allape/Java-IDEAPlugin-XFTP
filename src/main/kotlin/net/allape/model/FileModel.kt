package net.allape.model

enum class FileModelType {
    // 文件或文件夹, 是能读到内容的东西
    FILE,
    // 非文件夹, 比如列表分隔符或者返回上一级目录的占位符
    NON_FILE
}

data class FileModel(
    var path: String,
    var name: String,
    var directory: Boolean = false,
    var size: Long = 0L,
    var permissions: Int = 0,
    var local: Boolean = true,
    var type: FileModelType = FileModelType.FILE,
) {
    override fun toString(): String = (if (directory) "📁" else "📃") + " " + name
}
