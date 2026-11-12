package com.opticsappbg.opticsapp.web.dto;

import java.time.LocalDateTime;

public class BackupInfo {

    private final String fileName;
    private final LocalDateTime date;
    private final String size;

    public BackupInfo(String fileName, LocalDateTime date, String size) {
        this.fileName = fileName;
        this.date = date;
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getSize() {
        return size;
    }
}
