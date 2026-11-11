package com.opticsappbg.opticsapp.config;

import com.opticsappbg.opticsapp.security.AuthenticationDetails;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ActiveSidebarAdvice {

    private final UserService userService;

    public ActiveSidebarAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("activeSection")
    public String activeSection(HttpServletRequest request) {

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        if (uri.equals("/home") || uri.equals("/")) {
            return "home";
        }

        if (uri.startsWith("/products")) {
            return "products";
        }

        if (uri.startsWith("/customers")) {
            return "customers";
        }

        if (uri.startsWith("/orders")) {
            return "orders";
        }

        if (uri.startsWith("/deliveries")) {
            return "deliveries";
        }

        if (uri.startsWith("/reports")) {
            return "reports";
        }

        if (uri.startsWith("/system")) {
            return "system";
        }

        return "";
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticationDetails)) {
            return null;
        }

        return userService.getCurrentUser(authentication);
    }
}