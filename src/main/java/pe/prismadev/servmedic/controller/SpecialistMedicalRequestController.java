package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.AttentionReportResponse;
import pe.prismadev.servmedic.dto.MedicalRequestResponse;
import pe.prismadev.servmedic.dto.SaveAttentionReportRequest;
import pe.prismadev.servmedic.dto.SaveSpecialistAttentionReportRequest;
import pe.prismadev.servmedic.service.MedicalAttentionReportService;
import pe.prismadev.servmedic.service.MedicalRequestHistoryService;
import pe.prismadev.servmedic.service.MedicalRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/specialist/medical-requests")
public class SpecialistMedicalRequestController {

    private final MedicalRequestService medicalRequestService;
    private final MedicalRequestHistoryService medicalRequestHistoryService;
    private final MedicalAttentionReportService medicalAttentionReportService;

    public SpecialistMedicalRequestController(
        MedicalRequestService medicalRequestService,
        MedicalRequestHistoryService medicalRequestHistoryService,
        MedicalAttentionReportService medicalAttentionReportService
    ) {
        this.medicalRequestService = medicalRequestService;
        this.medicalRequestHistoryService = medicalRequestHistoryService;
        this.medicalAttentionReportService = medicalAttentionReportService;
    }

    @GetMapping("/pending")
    public List<MedicalRequestResponse> listPending(Authentication authentication) {
        Long specialistProfileId = getRequiredLongClaim(authentication, "specialistProfileId");
        return medicalRequestService.listPendingForAuthenticatedSpecialist(specialistProfileId);
    }

    @GetMapping("/assigned")
    public List<MedicalRequestResponse> listAssigned(
        Authentication authentication,
        @RequestParam(required = false) String status
    ) {
        Long specialistProfileId = getRequiredLongClaim(authentication, "specialistProfileId");
        return medicalRequestHistoryService.listSpecialistAssignedRequests(specialistProfileId, status);
    }

    @PatchMapping("/{requestId}/accept")
    public MedicalRequestResponse accept(
        Authentication authentication,
        @PathVariable Long requestId
    ) {
        Long specialistProfileId = getRequiredLongClaim(authentication, "specialistProfileId");
        return medicalRequestService.acceptRequest(requestId, specialistProfileId);
    }

    @PatchMapping("/{requestId}/start-route")
    public MedicalRequestResponse startRoute(
        Authentication authentication,
        @PathVariable Long requestId
    ) {
        Long specialistProfileId = getRequiredLongClaim(authentication, "specialistProfileId");
        return medicalRequestService.startRoute(requestId, specialistProfileId);
    }

    @PatchMapping("/{requestId}/start-attention")
    public MedicalRequestResponse startAttention(
        Authentication authentication,
        @PathVariable Long requestId
    ) {
        Long specialistProfileId = getRequiredLongClaim(authentication, "specialistProfileId");
        return medicalRequestService.startAttention(requestId, specialistProfileId);
    }

    @PatchMapping("/{requestId}/finish")
    public MedicalRequestResponse finish(
        Authentication authentication,
        @PathVariable Long requestId
    ) {
        Long specialistProfileId = getRequiredLongClaim(authentication, "specialistProfileId");
        return medicalRequestService.finish(requestId, specialistProfileId);
    }

    @PutMapping("/{requestId}/attention-report")
    public AttentionReportResponse saveAttentionReport(
        Authentication authentication,
        @PathVariable Long requestId,
        @Valid @RequestBody SaveSpecialistAttentionReportRequest request
    ) {
        Long specialistProfileId = getRequiredLongClaim(authentication, "specialistProfileId");

        SaveAttentionReportRequest secureRequest = new SaveAttentionReportRequest(
            specialistProfileId,
            request.clinicalObservations(),
            request.diagnosticImpression(),
            request.recommendations(),
            request.indications(),
            request.vitalSigns(),
            request.attachmentUrl()
        );

        return medicalAttentionReportService.saveReport(requestId, secureRequest);
    }

    @GetMapping("/{requestId}/attention-report")
    public AttentionReportResponse findAttentionReport(
        Authentication authentication,
        @PathVariable Long requestId
    ) {
        Long specialistProfileId = getRequiredLongClaim(authentication, "specialistProfileId");

        AttentionReportResponse response = medicalAttentionReportService.findByMedicalRequestId(requestId);

        if (response.specialistProfileId() == null || !response.specialistProfileId().equals(specialistProfileId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo el especialista asignado puede consultar la ficha de atencion."
            );
        }

        return response;
    }

    private Long getRequiredLongClaim(Authentication authentication, String claimName) {
        if (authentication == null || !(authentication.getDetails() instanceof Claims claims)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token no valido o sin claims."
            );
        }

        Object value = claims.get(claimName);

        if (value == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "El token no contiene el claim requerido: " + claimName
            );
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.valueOf(value.toString());
    }
}