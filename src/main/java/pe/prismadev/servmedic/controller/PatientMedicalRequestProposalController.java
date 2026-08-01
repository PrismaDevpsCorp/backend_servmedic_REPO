package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.MedicalRequestProposalResponse;
import pe.prismadev.servmedic.service.MedicalRequestProposalService;

import java.util.List;

@RestController
@RequestMapping(
    "/api/patient/medical-requests/{medicalRequestId}/proposals"
)
public class PatientMedicalRequestProposalController {

    private final MedicalRequestProposalService proposalService;

    public PatientMedicalRequestProposalController(
        MedicalRequestProposalService proposalService
    ) {
        this.proposalService = proposalService;
    }

    @GetMapping
    public List<MedicalRequestProposalResponse> list(
        Authentication authentication,
        @PathVariable Long medicalRequestId
    ) {
        Long patientProfileId = getRequiredLongClaim(
            authentication,
            "patientProfileId"
        );

        return proposalService.listForPatient(
            medicalRequestId,
            patientProfileId
        );
    }

    @PatchMapping("/{proposalId}/accept")
    public MedicalRequestProposalResponse accept(
        Authentication authentication,
        @PathVariable Long medicalRequestId,
        @PathVariable Long proposalId
    ) {
        Long patientProfileId = getRequiredLongClaim(
            authentication,
            "patientProfileId"
        );

        return proposalService.acceptForPatient(
            medicalRequestId,
            proposalId,
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
