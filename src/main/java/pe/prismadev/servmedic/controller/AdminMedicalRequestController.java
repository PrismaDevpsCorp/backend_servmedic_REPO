package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.prismadev.servmedic.dto.AdminMedicalRequestResponse;
import pe.prismadev.servmedic.service.AdminMedicalRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/medical-requests")
public class AdminMedicalRequestController {

    private final AdminMedicalRequestService adminMedicalRequestService;

    public AdminMedicalRequestController(AdminMedicalRequestService adminMedicalRequestService) {
        this.adminMedicalRequestService = adminMedicalRequestService;
    }

    @GetMapping
    public List<AdminMedicalRequestResponse> listMedicalRequests(
        @RequestParam(required = false) String status
    ) {
        return adminMedicalRequestService.listMedicalRequests(status);
    }
}