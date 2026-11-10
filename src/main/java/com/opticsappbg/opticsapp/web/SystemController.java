package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class SystemController {

    private final UserService userService;

    public SystemController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/system")
    public ModelAndView getSystemPage(Authentication authentication) {

        ModelAndView modelAndView = new ModelAndView("system");

        modelAndView.addObject("activeSection", "system");
        modelAndView.addObject(
                "currentUser",
                userService.getCurrentUser(authentication)
        );

        return modelAndView;
    }
}
