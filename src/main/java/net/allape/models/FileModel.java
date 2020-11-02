package net.allape.models;

import java.io.Serializable;

public class FileModel implements Serializable {

    private String path;

    private String name;

    private Boolean isFolder;

    public FileModel() {
    }

    public FileModel(String path, String name, Boolean isFolder) {
        this.path = path;
        this.name = name;
        this.isFolder = isFolder;
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

    @Override
    public String toString() {
        return (this.isFolder ? "📁" : "📃") + " " + this.name;
    }

}
