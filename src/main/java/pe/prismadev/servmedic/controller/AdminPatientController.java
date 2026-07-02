package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.prismadev.servmedic.dto.AdminPatientResponse;
import pe.prismadev.servmedic.service.AdminPatientService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/patients")
public class AdminPatientController {

    private final AdminPatientService adminPatientService;

    public AdminPatientController(AdminPatientService adminPatientService) {
        this.adminPatientService = adminPatientService;
    }

    @GetMapping
    public List<AdminPatientResponse> listPatients(
        @RequestParam(required = false) String search
    ) {
        return adminPatientService.listPatients(search);
    }
}