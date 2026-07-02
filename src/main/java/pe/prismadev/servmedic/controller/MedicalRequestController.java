package pe.prismadev.servmedic.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.CreateMedicalRequestRequest;
import pe.prismadev.servmedic.dto.MedicalRequestResponse;
import pe.prismadev.servmedic.service.MedicalRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/public/medical-requests")
public class MedicalRequestController {

    private final MedicalRequestService medicalRequestService;

    public MedicalRequestController(MedicalRequestService medicalRequestService) {
        this.medicalRequestService = medicalRequestService;
    }

    @PostMapping
    public MedicalRequestResponse create(@Valid @RequestBody CreateMedicalRequestRequest request) {
        return medicalRequestService.create(request);
    }

    @PatchMapping("/{requestId}/accept")
    public MedicalRequestResponse accept(
        @PathVariable Long requestId,
        @RequestParam Long specialistProfileId
    ) {
        return medicalRequestService.acceptRequest(requestId, specialistProfileId);
    }

    @PatchMapping("/{requestId}/start-route")
    public MedicalRequestResponse startRoute(
        @PathVariable Long requestId,
        @RequestParam Long specialistProfileId
    ) {
        return medicalRequestService.startRoute(requestId, specialistProfileId);
    }

    @PatchMapping("/{requestId}/start-attention")
    public MedicalRequestResponse startAttention(
        @PathVariable Long requestId,
        @RequestParam Long specialistProfileId
    ) {
        return medicalRequestService.startAttention(requestId, specialistProfileId);
    }

    @PatchMapping("/{requestId}/finish")
    public MedicalRequestResponse finish(
        @PathVariable Long requestId,
        @RequestParam Long specialistProfileId
    ) {
        return medicalRequestService.finish(requestId, specialistProfileId);
    }

    @GetMapping("/{id}")
    public MedicalRequestResponse findById(@PathVariable Long id) {
        return medicalRequestService.findById(id);
    }

    @GetMapping("/pending")
    public List<MedicalRequestResponse> listPendingByProfession(
        @RequestParam String professionCode
    ) {
        return medicalRequestService.listPendingByProfession(professionCode);
    }
}