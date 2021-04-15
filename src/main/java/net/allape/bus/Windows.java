package net.allape.bus;

import com.intellij.ui.content.Content;
import net.allape.windows.XFTPWindow;

import java.util.*;

public final class Windows {

    // 当前打开的窗口
    public static final Map<Content, XFTPWindow> windows = new HashMap<>(10);

}
