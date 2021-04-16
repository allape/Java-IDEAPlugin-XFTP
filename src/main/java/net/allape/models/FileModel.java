package net.allape.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileModel implements Serializable {

    /**
     * 路径
     */
    @NotNull
    private String path;

    /**
     * 文件名称
     */
    @NotNull
    private String name;

    /**
     * 是否为文件夹
     */
    private boolean directory = false;

    /**
     * 大小
     */
    private long size = 0L;

    /**
     * 权限
     */
    private int permissions = 0;

    /**
     * 是否为本地文件
     */
    private boolean local = true;

    @Override
    public String toString() {
        return (this.directory ? "📁" : "📃") + " " + this.name;
    }

}
