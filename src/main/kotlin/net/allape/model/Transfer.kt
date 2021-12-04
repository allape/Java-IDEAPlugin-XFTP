package net.allape.model

/**
 * 传输类型
 */
enum class TransferType {
    DOWNLOAD, UPLOAD
}

/**
 * 传输结果
 */
enum class TransferResult {
    // 成功
    SUCCESS,

    // 失败
    FAIL,

    // 传输中
    TRANSFERRING,

    // 取消
    CANCELLED
}

data class Transfer(
    /**
     * 传输类型
     */
    var type: TransferType,
    /**
     * 数据源
     */
    var source: String,
    /**
     * 目标
     */
    var target: String,
    /**
     * 大小
     */
    var size: Long,
    /**
     * 传输了的大小
     */
    var transferred: Long = 0,
    /**
     * 传输结果
     */
    var result: TransferResult = TransferResult.TRANSFERRING,
    /**
     * 错误信息
     */
    var exception: String? = null,
)
