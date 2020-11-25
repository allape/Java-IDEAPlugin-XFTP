package net.allape.bus;

import com.intellij.util.messages.Topic;

public interface HistoryTopicHandler {

    Topic<HistoryTopicHandler> HISTORY_TOPIC = Topic.create("history topic", HistoryTopicHandler.class);

    void before (HAction action);

    void after (HAction action);

    enum HAction {
        // 重新渲染传输历史窗口的内容
        RERENDER,
    }

}
