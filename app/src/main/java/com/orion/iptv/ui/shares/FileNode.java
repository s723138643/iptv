package com.orion.iptv.ui.shares;

import java.io.Serializable;

public class FileNode implements Serializable {
    static final long serialVersionUID = 27835L;

    public static final FileNode PARENT = new FileNode("..", "", false);
    public static final FileNode CURRENT = new FileNode(".", "", false);

    private final String name;
    private final boolean isFile;
    private final String path;

    public FileNode(String name, String absolutePath, boolean isFile) {
        this.name = (name != null && !name.equals("")) ? name : "";
        // change absolute path to relative path
        this.path = absolutePath.startsWith("/") ? absolutePath.substring(1) : absolutePath;
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
}
