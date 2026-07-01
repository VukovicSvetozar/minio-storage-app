package com.minio.storage.util;

public class FormatUtils {

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;

    private FormatUtils() {
    }

    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < KB) {
            return sizeInBytes + " B";
        }
        if (sizeInBytes < MB) {
            return String.format("%.2f KB", sizeInBytes / (double) KB);
        }
        if (sizeInBytes < GB) {
            return String.format("%.2f MB", sizeInBytes / (double) MB);
        }
        return String.format("%.2f GB", sizeInBytes / (double) GB);
    }

}