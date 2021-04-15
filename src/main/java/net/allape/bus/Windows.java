package net.allape.bus;

import com.intellij.ui.content.Content;
import net.allape.windows.XFTPWindow;

import java.util.*;

public final class Windows {

    /**
     * 默认的窗口名称
     */
    public static final String WINDOW_DEFAULT_NAME = "Explorer";

    // 当前打开的窗口
    public static final Map<Content, XFTPWindow> windows = new HashMap<>(10);

}
