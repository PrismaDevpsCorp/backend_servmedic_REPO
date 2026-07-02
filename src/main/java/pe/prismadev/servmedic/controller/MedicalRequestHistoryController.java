package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.MedicalRequestResponse;
import pe.prismadev.servmedic.service.MedicalRequestHistoryService;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class MedicalRequestHistoryController {

    private final MedicalRequestHistoryService medicalRequestHistoryService;

    public MedicalRequestHistoryController(MedicalRequestHistoryService medicalRequestHistoryService) {
        this.medicalRequestHistoryService = medicalRequestHistoryService;
    }

    @GetMapping("/patients/{patientProfileId}/medical-requests")
    public List<MedicalRequestResponse> listPatientRequests(
        @PathVariable Long patientProfileId,
        @RequestParam(required = false) String status
    ) {
        return medicalRequestHistoryService.listPatientRequests(patientProfileId, status);
    }

    @GetMapping("/specialists/{specialistProfileId}/assigned-requests")
    public List<MedicalRequestResponse> listSpecialistAssignedRequests(
        @PathVariable Long specialistProfileId,
        @RequestParam(required = false) String status
    ) {
        return medicalRequestHistoryService.listSpecialistAssignedRequests(specialistProfileId, status);
    }
}