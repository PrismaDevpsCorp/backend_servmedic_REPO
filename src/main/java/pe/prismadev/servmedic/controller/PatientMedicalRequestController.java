package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CreatePatientMedicalRequestRequest;
import pe.prismadev.servmedic.dto.MedicalRequestResponse;
import pe.prismadev.servmedic.service.MedicalRequestHistoryService;
import pe.prismadev.servmedic.service.MedicalRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/patient/medical-requests")
public class PatientMedicalRequestController {

    private final MedicalRequestService medicalRequestService;
    private final MedicalRequestHistoryService medicalRequestHistoryService;

    public PatientMedicalRequestController(
        MedicalRequestService medicalRequestService,
        MedicalRequestHistoryService medicalRequestHistoryService
    ) {
        this.medicalRequestService = medicalRequestService;
        this.medicalRequestHistoryService = medicalRequestHistoryService;
    }

    @PostMapping
    public MedicalRequestResponse create(
        Authentication authentication,
        @Valid @RequestBody CreatePatientMedicalRequestRequest request
    ) {
        Long patientProfileId = getRequiredLongClaim(authentication, "patientProfileId");
        return medicalRequestService.createForAuthenticatedPatient(patientProfileId, request);
    }

    @GetMapping
    public List<MedicalRequestResponse> list(
        Authentication authentication,
        @RequestParam(required = false) String status
    ) {
        Long patientProfileId = getRequiredLongClaim(authentication, "patientProfileId");
        return medicalRequestHistoryService.listPatientRequests(patientProfileId, status);
    }

    @GetMapping("/{id}")
    public MedicalRequestResponse findById(
        Authentication authentication,
        @PathVariable Long id
    ) {
        Long patientProfileId = getRequiredLongClaim(authentication, "patientProfileId");
        return medicalRequestService.findPatientRequestById(patientProfileId, id);
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