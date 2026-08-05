package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CreateMedicalRequestAdditionalRequest;
import pe.prismadev.servmedic.dto.MedicalRequestAdditionalResponse;
import pe.prismadev.servmedic.service.MedicalRequestAdditionalService;

import java.util.List;

@RestController
@RequestMapping(
    "/api/specialist/medical-requests/{medicalRequestId}/additionals"
)
public class SpecialistMedicalRequestAdditionalController {

    private final MedicalRequestAdditionalService additionalService;

    public SpecialistMedicalRequestAdditionalController(
        MedicalRequestAdditionalService additionalService
    ) {
        this.additionalService = additionalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalRequestAdditionalResponse create(
        Authentication authentication,
        @PathVariable Long medicalRequestId,
        @Valid @RequestBody
        CreateMedicalRequestAdditionalRequest request
    ) {
        Long specialistProfileId = getRequiredLongClaim(
            authentication,
            "specialistProfileId"
        );

        return additionalService.createForSpecialist(
            medicalRequestId,
            specialistProfileId,
            request
        );
    }

    @GetMapping
    public List<MedicalRequestAdditionalResponse> list(
        Authentication authentication,
        @PathVariable Long medicalRequestId
    ) {
        Long specialistProfileId = getRequiredLongClaim(
            authentication,
            "specialistProfileId"
        );

        return additionalService.listForSpecialist(
            medicalRequestId,
            specialistProfileId
        );
    }

    @PatchMapping("/{additionalId}/withdraw")
    public MedicalRequestAdditionalResponse withdraw(
        Authentication authentication,
        @PathVariable Long medicalRequestId,
        @PathVariable Long additionalId
    ) {
        Long specialistProfileId = getRequiredLongClaim(
            authentication,
            "specialistProfileId"
        );

        return additionalService.withdrawForSpecialist(
            medicalRequestId,
            additionalId,
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
