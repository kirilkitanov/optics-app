package com.opticsappbg.opticsapp.activity.web;

import com.opticsappbg.opticsapp.activity.model.ActivityAction;
import com.opticsappbg.opticsapp.activity.service.ActivityLogService;
import com.opticsappbg.opticsapp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final UserService userService;

    private static final List<String> ENTITY_TYPES = List.of(
            "PRODUCT",
            "ORDER",
            "DELIVERY",
            "CUSTOMER_PAYMENT",
            "SUPPLIER_PAYMENT",
            "CUSTOMER",
            "SUPPLIER",
            "USER"
    );

    @GetMapping
    public String showActivityHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) ActivityAction action,
            @RequestParam(required = false) String entityType,
            Model model
    ) {
        LocalDateTime dateTimeFrom = dateFrom != null
                ? dateFrom.atStartOfDay()
                : null;

        LocalDateTime dateTimeTo = dateTo != null
                ? dateTo.atTime(LocalTime.MAX)
                : null;

        String selectedEntityType = entityType != null && !entityType.isBlank()
                ? entityType
                : null;

        model.addAttribute(
                "logs",
                activityLogService.findLogs(
                        userId,
                        dateTimeFrom,
                        dateTimeTo,
                        action,
                        selectedEntityType
                )
        );

        model.addAttribute("users", userService.getAllUsersForActivityFilter());
        model.addAttribute("actions", ActivityAction.values());
        model.addAttribute("entityTypes", ENTITY_TYPES);

        model.addAttribute("selectedUserId", userId);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("selectedAction", action);
        model.addAttribute("selectedEntityType", selectedEntityType);

        return "activity/activity-history";
    }
}
