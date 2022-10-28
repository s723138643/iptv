package com.orion.iptv.ui.shares;

public class FileNode {
    private final String name;
    private final boolean isFile;
    private final FileNode parent;
    private final String path;

    public FileNode(String name, String absolutePath, boolean isFile, FileNode parent) {
        this.name = (name != null && !name.equals("")) ? name : "";
        // change absolute path to relative path
        this.path = absolutePath.startsWith("/") ? absolutePath.substring(1) : absolutePath;
        this.parent = parent;
        this.isFile = isFile;
    }

    public String getPath() {
        return path;
    }

    public String getAbsolutePath() {
        return "/" + path;
    }

    public String getName() {
        return name;
    }

    public boolean isFile() {
        return isFile;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public FileNode backup() {
        return parent;
    }
}
