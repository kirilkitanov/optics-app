
package com.opticsappbg.opticsapp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ActiveSidebarAdvice {

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
}