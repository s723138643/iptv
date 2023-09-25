package com.orion.iptv.ui.shares;

import android.os.Build;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class FileNode implements Serializable {
    static final long serialVersionUID = 27835L;

    public static final FileNode PARENT = new FileNode("..", "", false, -2, null);
    public static final FileNode CURRENT = new FileNode(".", "", false, -3, null);

    private final String name;
    private final boolean isFile;
    private final String path;
    private final long size;
    private final Date lastModified;
    private final String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
    public FileNode(String name, String absolutePath, boolean isFile, long size, @Nullable Date lastModified) {
        this.name = (name != null && !name.equals("")) ? name : "";
        // change absolute path to relative path
        this.path = absolutePath.startsWith("/") ? absolutePath.substring(1) : absolutePath;
        this.isFile = isFile;
        // directories placed before or after files when sorted by size
        this.size = isFile ? (size > 0 ? size : 0) : -1;
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

    public boolean isDummy() {
        return this == CURRENT || this == PARENT;
    }

    public String getSize(String directoryPlaceholder) {
        if (isDummy()) {
            return "";
        } else if (!isFile()) {
            return directoryPlaceholder;
        }
        return formatSize(size);
    }

    public String getLastModified(DateFormat formatter) {
        if (isDummy() || lastModified==null) {
            return "";
        }
        return formatter.format(lastModified);
    }

    public static class CompareByName implements Comparator<FileNode> {
        @Override
        public int compare(FileNode o1, FileNode o2) {
            return o1.getName().compareTo(o2.getName());
        }

        public static Comparator<FileNode> reverse() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return new CompareByName().reversed();
            } else {
                Comparator<FileNode> comparator = new CompareByName();
                return (o1, o2) -> comparator.compare(o2, o1);
            }
        }
    }

    public static class CompareBySize implements Comparator<FileNode> {
        @Override
        public int compare(FileNode o1, FileNode o2) {
            return Long.compare(o1.size, o2.size);
        }

        public static Comparator<FileNode> reverse() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return new CompareBySize().reversed();
            } else {
                Comparator<FileNode> comparator = new CompareBySize();
                return (o1, o2) -> comparator.compare(o2, o1);
            }
        }
    }

    public static class CompareByModified implements Comparator<FileNode> {
        @Override
        public int compare(FileNode o1, FileNode o2) {
            Date o1Date = o1.lastModified == null ? new Date(0) : o1.lastModified;
            Date o2Date = o2.lastModified == null ? new Date(0) : o2.lastModified;
            return o1Date.compareTo(o2Date);
        }

        public static Comparator<FileNode> reverse() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return new CompareByModified().reversed();
            } else {
                Comparator<FileNode> comparator = new CompareByModified();
                return (o1, o2) -> comparator.compare(o2, o1);
            }
        }
    }
}
