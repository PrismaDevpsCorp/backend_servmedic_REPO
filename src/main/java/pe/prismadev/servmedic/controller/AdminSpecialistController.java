package pe.prismadev.servmedic.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.AdminSpecialistResponse;
import pe.prismadev.servmedic.dto.UpdateSpecialistStatusRequest;
import pe.prismadev.servmedic.service.AdminSpecialistService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/specialists")
public class AdminSpecialistController {

    private final AdminSpecialistService adminSpecialistService;

    public AdminSpecialistController(AdminSpecialistService adminSpecialistService) {
        this.adminSpecialistService = adminSpecialistService;
    }

    @GetMapping
    public List<AdminSpecialistResponse> listSpecialists(
        @RequestParam(required = false) String status
    ) {
        return adminSpecialistService.listSpecialists(status);
    }

    @PatchMapping("/{specialistProfileId}/status")
    public AdminSpecialistResponse updateStatus(
        @PathVariable Long specialistProfileId,
        @Valid @RequestBody UpdateSpecialistStatusRequest request
    ) {
        return adminSpecialistService.updateStatus(specialistProfileId, request);
    }
}