package net.allape.models;

import java.io.Serializable;

public class FileModel implements Serializable {

    private String path;

    private String name;

    private Boolean isFolder;

    private Long size;

    private Integer permissions;

    public FileModel() {
    }

    public FileModel(String path, String name, Boolean isFolder, Long size, Integer permissions) {
        this.path = path;
        this.name = name;
        this.isFolder = isFolder;
        this.size = size;
        this.permissions = permissions;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getFolder() {
        return isFolder;
    }

    public void setFolder(Boolean folder) {
        isFolder = folder;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Integer getPermissions() {
        return permissions;
    }

    public void setPermissions(Integer permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return (this.isFolder ? "📁" : "📃") + " " + this.name;
    }

}
