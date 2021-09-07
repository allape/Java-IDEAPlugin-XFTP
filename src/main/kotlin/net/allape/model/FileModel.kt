package net.allape.model

data class FileModel(
    var path: String,
    var name: String,
    var directory: Boolean = false,
    var size: Long = 0L,
    var permissions: Int = 0,
    var local: Boolean = true,
) {
    override fun toString(): String = (if (directory) "📁" else "📃") + " " + name
}
