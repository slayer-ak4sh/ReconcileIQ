package com.reconcileiq.dashboard;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')")
    public java.util.Map<String, Object> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return dashboardService.getSummary(asOfDate);
    }

    @GetMapping("/money-at-risk")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')")
    public java.util.List<java.util.Map<String, Object>> getMoneyAtRisk(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return dashboardService.getMoneyAtRiskByPartnerAndAge(asOfDate);
    }
}