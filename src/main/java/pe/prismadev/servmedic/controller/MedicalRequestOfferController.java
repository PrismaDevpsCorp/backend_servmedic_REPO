package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.MedicalRequestResponse;
import pe.prismadev.servmedic.dto.RequestOfferResponse;
import pe.prismadev.servmedic.service.MedicalRequestOfferService;

@RestController
@RequestMapping("/api/public/medical-requests")
public class MedicalRequestOfferController {

    private final MedicalRequestOfferService medicalRequestOfferService;

    public MedicalRequestOfferController(MedicalRequestOfferService medicalRequestOfferService) {
        this.medicalRequestOfferService = medicalRequestOfferService;
    }

    @PostMapping("/{medicalRequestId}/offer-to-specialist")
    public RequestOfferResponse offerToSpecialist(
        @PathVariable Long medicalRequestId,
        @RequestParam Long specialistProfileId,
        @RequestParam(defaultValue = "30") int ttlSeconds
    ) {
        return medicalRequestOfferService.offerRequestToSpecialist(
            medicalRequestId,
            specialistProfileId,
            ttlSeconds
        );
    }

    @GetMapping("/{medicalRequestId}/offer-status")
    public RequestOfferResponse getOfferStatus(
        @PathVariable Long medicalRequestId,
        @RequestParam Long specialistProfileId
    ) {
        return medicalRequestOfferService.getOfferStatus(medicalRequestId, specialistProfileId);
    }

    @PatchMapping("/{medicalRequestId}/accept-offer")
    public MedicalRequestResponse acceptOffer(
        @PathVariable Long medicalRequestId,
        @RequestParam Long specialistProfileId
    ) {
        return medicalRequestOfferService.acceptOffer(medicalRequestId, specialistProfileId);
    }
}