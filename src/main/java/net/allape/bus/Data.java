package net.allape.bus;

import com.intellij.ui.content.Content;
import net.allape.models.Transfer;
import net.allape.windows.XFTPWindow;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public final class Data {

    // 当前打开的窗口
    public static final Map<Content, XFTPWindow> windows = new HashMap<>(10);

    // 传输日志
    public static final List<Transfer> HISTORY = new ArrayList<>(100);

    // 当前传输中的内容
    public static final Queue<Transfer> TRANSFERRING = new LinkedBlockingQueue<>(100);

}
