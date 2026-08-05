package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.MedicalRequestAdditionalResponse;
import pe.prismadev.servmedic.service.MedicalRequestAdditionalService;

import java.util.List;

@RestController
@RequestMapping(
    "/api/patient/medical-requests/{medicalRequestId}/additionals"
)
public class PatientMedicalRequestAdditionalController {

    private final MedicalRequestAdditionalService additionalService;

    public PatientMedicalRequestAdditionalController(
        MedicalRequestAdditionalService additionalService
    ) {
        this.additionalService = additionalService;
    }

    @GetMapping
    public List<MedicalRequestAdditionalResponse> list(
        Authentication authentication,
        @PathVariable Long medicalRequestId
    ) {
        Long patientProfileId = getRequiredLongClaim(
            authentication,
            "patientProfileId"
        );

        return additionalService.listForPatient(
            medicalRequestId,
            patientProfileId
        );
    }

    @PatchMapping("/{additionalId}/approve")
    public MedicalRequestAdditionalResponse approve(
        Authentication authentication,
        @PathVariable Long medicalRequestId,
        @PathVariable Long additionalId
    ) {
        Long patientProfileId = getRequiredLongClaim(
            authentication,
            "patientProfileId"
        );

        return additionalService.approveForPatient(
            medicalRequestId,
            additionalId,
            patientProfileId
        );
    }

    @PatchMapping("/{additionalId}/reject")
    public MedicalRequestAdditionalResponse reject(
        Authentication authentication,
        @PathVariable Long medicalRequestId,
        @PathVariable Long additionalId
    ) {
        Long patientProfileId = getRequiredLongClaim(
            authentication,
            "patientProfileId"
        );

        return additionalService.rejectForPatient(
            medicalRequestId,
            additionalId,
            patientProfileId
        );
    }

    private Long getRequiredLongClaim(
        Authentication authentication,
        String claimName
    ) {
        if (
            authentication == null
                || !(authentication.getDetails() instanceof Claims claims)
        ) {
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

        try {
            return Long.valueOf(value.toString());
        }
        catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "El claim requerido no contiene un identificador valido: "
                    + claimName,
                exception
            );
        }
    }
}
