package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CreateMedicalRequestProposalRequest;
import pe.prismadev.servmedic.dto.MedicalRequestProposalResponse;
import pe.prismadev.servmedic.service.MedicalRequestProposalService;

import java.util.List;

@RestController
@RequestMapping(
    "/api/specialist/medical-requests/{medicalRequestId}/proposals"
)
public class SpecialistMedicalRequestProposalController {

    private final MedicalRequestProposalService proposalService;

    public SpecialistMedicalRequestProposalController(
        MedicalRequestProposalService proposalService
    ) {
        this.proposalService = proposalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalRequestProposalResponse create(
        Authentication authentication,
        @PathVariable Long medicalRequestId,
        @Valid @RequestBody CreateMedicalRequestProposalRequest request
    ) {
        Long specialistProfileId = getRequiredLongClaim(
            authentication,
            "specialistProfileId"
        );

        return proposalService.createProposal(
            medicalRequestId,
            specialistProfileId,
            request
        );
    }

    @GetMapping
    public List<MedicalRequestProposalResponse> list(
        Authentication authentication,
        @PathVariable Long medicalRequestId
    ) {
        Long specialistProfileId = getRequiredLongClaim(
            authentication,
            "specialistProfileId"
        );

        return proposalService.listForSpecialist(
            medicalRequestId,
            specialistProfileId
        );
    }

    @PatchMapping("/{proposalId}/withdraw")
    public MedicalRequestProposalResponse withdraw(
        Authentication authentication,
        @PathVariable Long medicalRequestId,
        @PathVariable Long proposalId
    ) {
        Long specialistProfileId = getRequiredLongClaim(
            authentication,
            "specialistProfileId"
        );

        return proposalService.withdrawForSpecialist(
            medicalRequestId,
            proposalId,
            specialistProfileId
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
