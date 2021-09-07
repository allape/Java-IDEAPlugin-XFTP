package net.allape.common

import com.intellij.util.messages.Topic

interface HistoryTopicHandler {

    companion object {

        var HISTORY_TOPIC = Topic.create("history topic", HistoryTopicHandler::class.java)

    }

    fun before(action: HAction?)

    fun after(action: HAction?)

    enum class HAction {
        // 重新渲染传输历史窗口的内容
        RERENDER
    }
}