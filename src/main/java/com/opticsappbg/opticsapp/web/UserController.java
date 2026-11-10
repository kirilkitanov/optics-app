
package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.model.UserRole;
import com.opticsappbg.opticsapp.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Списък с потребители
    @GetMapping
    public ModelAndView getUsers(Authentication authentication) {

        User currentUser = userService.getCurrentUser(authentication);

        ModelAndView modelAndView = new ModelAndView("users/users-list");

        modelAndView.addObject("users",
                userService.getUsers(authentication));

        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("userRoles", UserRole.values());

        return modelAndView;
    }

    // Създаване на потребител
    @PostMapping("/create")
    public String createUser(
            @RequestParam String username,
            @RequestParam(required = false) String fullName,
            @RequestParam String password,
            @RequestParam UserRole role,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            userService.createUser(
                    username, fullName, password, role, authentication);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Потребителят е създаден успешно.");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/users";
    }

    // Редактиране на профил
    @PostMapping("/{id}/edit")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam String username,
            @RequestParam(required = false) String fullName,
            @RequestParam UserRole role,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            userService.updateUser(
                    id, username, fullName, role, authentication);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Профилът е редактиран успешно.");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/users";
    }

    // Промяна на парола
    @PostMapping("/{id}/change-password")
    public String changePassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            userService.changePassword(
                    id, newPassword, authentication);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Паролата е променена успешно.");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/users";
    }

    // Деактивиране на профил
    @PostMapping("/{id}/deactivate")
    public String deactivateUser(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            userService.deactivateUser(id, authentication);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Профилът е деактивиран.");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/users";
    }

    // Възстановяване на профил – само Admin
    @PostMapping("/{id}/reactivate")
    public String reactivateUser(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            userService.reactivateUser(id, authentication);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Профилът е възстановен успешно.");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/users";
    }

    // Страница за редакция на потребител
    @GetMapping("/{id}/edit")
    public ModelAndView editUser(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userService.getUserForManagement(id, authentication);
            User currentUser = userService.getCurrentUser(authentication);

            ModelAndView modelAndView = new ModelAndView("users/user-form");
            modelAndView.addObject("user", user);
            modelAndView.addObject("currentUser", currentUser);

            return modelAndView;

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return new ModelAndView("redirect:/users");
        }
    }
}
