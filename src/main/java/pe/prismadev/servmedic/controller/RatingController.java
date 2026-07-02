package pe.prismadev.servmedic.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.CreatePatientRatingRequest;
import pe.prismadev.servmedic.dto.CreateSpecialistRatingRequest;
import pe.prismadev.servmedic.dto.MedicalRequestRatingsResponse;
import pe.prismadev.servmedic.dto.RatingResponse;
import pe.prismadev.servmedic.service.RatingService;

@RestController
@RequestMapping("/api/public/medical-requests")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/{medicalRequestId}/ratings/patient-to-specialist")
    public RatingResponse rateSpecialist(
        @PathVariable Long medicalRequestId,
        @Valid @RequestBody CreatePatientRatingRequest request
    ) {
        return ratingService.rateSpecialist(medicalRequestId, request);
    }

    @PostMapping("/{medicalRequestId}/ratings/specialist-to-patient")
    public RatingResponse ratePatient(
        @PathVariable Long medicalRequestId,
        @Valid @RequestBody CreateSpecialistRatingRequest request
    ) {
        return ratingService.ratePatient(medicalRequestId, request);
    }

    @GetMapping("/{medicalRequestId}/ratings")
    public MedicalRequestRatingsResponse listRatings(
        @PathVariable Long medicalRequestId
    ) {
        return ratingService.listRatings(medicalRequestId);
    }
}