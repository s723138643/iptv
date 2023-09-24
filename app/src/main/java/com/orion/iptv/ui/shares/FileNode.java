package com.orion.iptv.ui.shares;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class FileNode implements Serializable {
    static final long serialVersionUID = 27835L;

    public static final FileNode PARENT = new FileNode("..", "", false, 0, null);
    public static final FileNode CURRENT = new FileNode(".", "", false, 0, null);

    private final String name;
    private final boolean isFile;
    private final String path;
    private final long size;
    private final Date lastModified;
    private final DateFormat formatter;

    private final String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
    public FileNode(String name, String absolutePath, boolean isFile, long size, @Nullable Date lastModified) {
        this.name = (name != null && !name.equals("")) ? name : "";
        // change absolute path to relative path
        this.path = absolutePath.startsWith("/") ? absolutePath.substring(1) : absolutePath;
        this.isFile = isFile;
        this.size = size;
        this.formatter = new SimpleDateFormat("yyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        this.lastModified = lastModified;
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

    private String formatSize(long size) {
        int i = 0;
        double s = (double) size;
        while (s > 1024) {
            i += 1;
            s = s / 1024;
        }
        return String.format(Locale.ENGLISH, "%.2f%s", s, units[i]);
    }

    public String getSize() {
        if (name.equals(".") || name.equals("..")) {
            return "";
        }
        return formatSize(size);
    }

    public String getLastModified() {
        if (name.equals(".") || name.equals("..") || lastModified==null) {
            return "";
        }
        return formatter.format(lastModified);
    }

    public static class CompareByName implements Comparator<FileNode> {
        @Override
        public int compare(FileNode o1, FileNode o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
