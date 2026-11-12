package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.web.dto.BackupInfo;
import com.opticsappbg.opticsapp.backup.service.BackupManagementService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequestMapping("/backups")
public class BackupController {

    private final BackupManagementService backupManagementService;

    public BackupController(BackupManagementService backupManagementService) {
        this.backupManagementService = backupManagementService;
    }

    @GetMapping
    public String showBackups(Model model) throws Exception {

        List<BackupInfo> backups = backupManagementService.getBackups()
                .stream()
                .map(path -> {
                    try {
                        String fileName = path.getFileName().toString();

                        LocalDateTime date = LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(path).toInstant(),
                                ZoneId.systemDefault()
                        );

                        long sizeInBytes = Files.size(path);

                        String size = String.format(
                                "%.2f MB",
                                sizeInBytes / (1024.0 * 1024.0)
                        );

                        return new BackupInfo(fileName, date, size);

                    } catch (Exception exception) {
                        throw new RuntimeException(
                                "Не може да се прочете информацията за архив: "
                                        + path.getFileName(),
                                exception
                        );
                    }
                })
                .toList();

        model.addAttribute("backups", backups);
        model.addAttribute(
                "backupDirectory",
                backupManagementService.getBackupDirectory()
        );

        return "backups/backups";
    }

    @PostMapping("/directory")
    public String setBackupDirectory(
            @RequestParam String directory,
            RedirectAttributes redirectAttributes) {

        try {
            backupManagementService.setBackupDirectory(directory);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Папката за архиви е променена успешно."
            );

        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Неуспешно запазване на папката: "
                            + exception.getMessage()
            );
        }

        return "redirect:/backups";
    }

    @PostMapping("/create")
    public String createBackup(RedirectAttributes redirectAttributes) {

        try {
            backupManagementService.createBackup();

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Архивът е създаден успешно."
            );

        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Възникна грешка при създаването на архива: "
                            + exception.getMessage()
            );
        }

        return "redirect:/backups";
    }

    @PostMapping("/delete/{fileName}")
    public String deleteBackup(
            @PathVariable String fileName,
            RedirectAttributes redirectAttributes) {

        try {
            backupManagementService.deleteBackup(fileName);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Архивът е изтрит успешно."
            );

        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Възникна грешка при изтриването: "
                            + exception.getMessage()
            );
        }

        return "redirect:/backups";
    }

    @PostMapping("/restore/{fileName}")
    public String restoreBackup(
            @PathVariable String fileName,
            RedirectAttributes redirectAttributes) {

        try {
            backupManagementService.restoreBackup(fileName);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Базата данни е възстановена успешно."
            );

        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Възстановяването не беше извършено: "
                            + exception.getMessage()
            );
        }

        return "redirect:/backups";
    }
}
