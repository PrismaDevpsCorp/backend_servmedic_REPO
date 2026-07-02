package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.prismadev.servmedic.dto.AdminPaymentResponse;
import pe.prismadev.servmedic.dto.AdminPaymentSummaryResponse;
import pe.prismadev.servmedic.service.AdminPaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    public AdminPaymentController(AdminPaymentService adminPaymentService) {
        this.adminPaymentService = adminPaymentService;
    }

    @GetMapping
    public List<AdminPaymentResponse> listPayments(
        @RequestParam(required = false) String status
    ) {
        return adminPaymentService.listPayments(status);
    }

    @GetMapping("/summary")
    public AdminPaymentSummaryResponse getSummary(
        @RequestParam(required = false) String status
    ) {
        return adminPaymentService.getSummary(status);
    }
}