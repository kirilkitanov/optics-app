package com.opticsappbg.opticsapp.backup.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

@Service
public class BackupService {

    private static final String SQLITE_PREFIX = "jdbc:sqlite:";
    private static final String BACKUP_PREFIX = "optics-backup-";
    private static final String BACKUP_EXTENSION = ".db";

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    private final Path databasePath;
    private final Path settingsFile;
    private final Path defaultBackupDirectory;

    private Path backupDirectory;

    public BackupService(@Value("${spring.datasource.url}") String databaseUrl) {

        if (!databaseUrl.startsWith(SQLITE_PREFIX)) {
            throw new IllegalStateException(
                    "Невалиден SQLite адрес на базата данни."
            );
        }

        String databaseFile = databaseUrl.substring(SQLITE_PREFIX.length());

        this.databasePath = Paths.get(databaseFile)
                .toAbsolutePath()
                .normalize();

        Path parentDirectory = databasePath.getParent();

        if (parentDirectory == null) {
            throw new IllegalStateException(
                    "Не може да се определи папката на базата данни."
            );
        }

        this.defaultBackupDirectory = parentDirectory.resolve("backups");
        this.settingsFile = parentDirectory.resolve("backup-settings.properties");

        this.backupDirectory = loadBackupDirectory();
    }

    private Path loadBackupDirectory() {

        Properties properties = new Properties();

        if (Files.isRegularFile(settingsFile)) {
            try (InputStream input = Files.newInputStream(settingsFile)) {
                properties.load(input);

                String savedPath = properties.getProperty("backup.directory");

                if (savedPath != null && !savedPath.isBlank()) {
                    return Paths.get(savedPath).toAbsolutePath().normalize();
                }

            } catch (Exception ignored) {
            }
        }

        return defaultBackupDirectory;
    }

    public synchronized void setBackupDirectory(String directory) throws Exception {

        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException("Моля, изберете папка.");
        }

        Path newDirectory = Paths.get(directory.trim())
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(newDirectory);

        Properties properties = new Properties();
        properties.setProperty("backup.directory", newDirectory.toString());

        try (OutputStream output = Files.newOutputStream(
                settingsFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            properties.store(output, "Backup settings");
        }

        this.backupDirectory = newDirectory;
    }

    public synchronized String getBackupDirectory() {
        return backupDirectory.toString();
    }

    public synchronized void createBackup() throws Exception {

        if (!Files.isRegularFile(databasePath)) {
            throw new IllegalStateException(
                    "Файлът на базата данни не е намерен."
            );
        }

        Files.createDirectories(backupDirectory);

        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);

        Path backupFile = backupDirectory.resolve(
                BACKUP_PREFIX + timestamp + BACKUP_EXTENSION
        );

        if (Files.exists(backupFile)) {
            throw new IllegalStateException(
                    "Вече съществува архив с това име. Опитайте отново."
            );
        }

        try (Connection connection = DriverManager.getConnection(
                SQLITE_PREFIX + databasePath
        );
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA busy_timeout = 10000");

            String destination = backupFile.toString().replace("'", "''");

            statement.execute("VACUUM INTO '" + destination + "'");
        }
    }

    public synchronized List<Path> getBackups() throws Exception {

        if (!Files.exists(backupDirectory)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(backupDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();

                        return name.startsWith(BACKUP_PREFIX)
                                && name.endsWith(BACKUP_EXTENSION);
                    })
                    .sorted(Comparator.reverseOrder())
                    .toList();
        }
    }

    public synchronized void deleteBackup(String fileName) throws Exception {

        Path backupFile = resolveBackupFile(fileName);

        if (!Files.isRegularFile(backupFile)) {
            throw new IllegalArgumentException("Архивът не е намерен.");
        }

        Files.delete(backupFile);
    }

    public synchronized boolean isValidBackup(String fileName) throws Exception {

        Path backupFile;

        try {
            backupFile = resolveBackupFile(fileName);
        } catch (Exception exception) {
            return false;
        }

        if (!Files.isRegularFile(backupFile)) {
            return false;
        }

        try (Connection connection = DriverManager.getConnection(
                SQLITE_PREFIX + backupFile
        );
             Statement statement = connection.createStatement();
             java.sql.ResultSet resultSet =
                     statement.executeQuery("PRAGMA integrity_check")) {

            return resultSet.next()
                    && "ok".equalsIgnoreCase(resultSet.getString(1));

        } catch (Exception exception) {
            return false;
        }
    }

    public synchronized void restoreBackup(String fileName) throws Exception {

        if (!isValidBackup(fileName)) {
            throw new IllegalArgumentException(
                    "Избраният архив е невалиден или не е намерен."
            );
        }

        Path backupFile = resolveBackupFile(fileName);

        try (Connection connection = DriverManager.getConnection(
                SQLITE_PREFIX + databasePath
        );
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA busy_timeout = 10000");

            String source = backupFile.toString().replace("'", "''");

            statement.execute("restore from '" + source + "'");
        }
    }

    private Path resolveBackupFile(String fileName) {

        Path backupFile = backupDirectory.resolve(fileName).normalize();

        if (!backupFile.getParent().equals(backupDirectory)) {
            throw new IllegalArgumentException("Невалиден път до архива.");
        }

        String name = backupFile.getFileName().toString();

        if (!name.startsWith(BACKUP_PREFIX)
                || !name.endsWith(BACKUP_EXTENSION)) {
            throw new IllegalArgumentException("Невалидно име на архив.");
        }

        return backupFile;
    }
}
