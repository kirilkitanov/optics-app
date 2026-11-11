
package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.backup.service.BackupManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/backups")
public class BackupController {

    private final BackupManagementService backupManagementService;

    public BackupController(BackupManagementService backupManagementService) {
        this.backupManagementService = backupManagementService;
    }

    @GetMapping
    public String showBackups(Model model) throws Exception {

        model.addAttribute(
                "backups",
                backupManagementService.getBackups().stream()
                        .map(path -> path.getFileName().toString())
                        .toList()
        );

        model.addAttribute(
                "backupDirectory",
                backupManagementService.getBackupDirectory()
        );

        return "backups/backups";
    }

    @PostMapping("/choose-directory")
    public String chooseBackupDirectory(RedirectAttributes redirectAttributes) {

        try {
            String directory = backupManagementService.chooseBackupDirectory();

            if (directory != null) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Папката за архиви е променена успешно."
                );
            }

        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Неуспешен избор на папка: " + exception.getClass().getName()
                            + " - " + exception.getMessage()
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
