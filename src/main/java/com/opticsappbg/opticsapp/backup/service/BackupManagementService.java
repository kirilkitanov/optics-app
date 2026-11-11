
package com.opticsappbg.opticsapp.backup.service;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class BackupManagementService {

    private final BackupService backupService;

    public BackupManagementService(BackupService backupService) {
        this.backupService = backupService;
    }

    public void restoreBackup(String fileName) throws Exception {
        backupService.restoreBackup(fileName);
    }

    public void createBackup() throws Exception {
        backupService.createBackup();
    }

    public List<Path> getBackups() throws Exception {
        return backupService.getBackups();
    }

    public void deleteBackup(String fileName) throws Exception {
        backupService.deleteBackup(fileName);
    }

    public boolean isValidBackup(String fileName) throws Exception {
        return backupService.isValidBackup(fileName);
    }

    public void setBackupDirectory(String directory) throws Exception {
        backupService.setBackupDirectory(directory);
    }

    public String getBackupDirectory() {
        return backupService.getBackupDirectory();
    }

    public String chooseBackupDirectory() throws Exception {
        return backupService.chooseBackupDirectory();
    }
}
