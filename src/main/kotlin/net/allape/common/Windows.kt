package net.allape.common

import com.intellij.ui.content.Content
import net.allape.xftp.Explorer

class Windows {

    companion object {

        // 默认的窗口名称
        const val WINDOW_DEFAULT_NAME = "Explorer"

        // 当前打开的窗口
        val windows: HashMap<Content, Explorer> = HashMap(10)

    }

}