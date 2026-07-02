package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.prismadev.servmedic.dto.DashboardSummaryResponse;
import pe.prismadev.servmedic.service.AdminDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return adminDashboardService.getSummary();
    }
}