package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.SpecialistCommercialProfileResponse;
import pe.prismadev.servmedic.dto.UpdateSpecialistCommercialProfileRequest;
import pe.prismadev.servmedic.service.SpecialistCommercialProfileService;

@RestController
@RequestMapping("/api/specialist/commercial-profile")
public class SpecialistCommercialProfileController {

    private final SpecialistCommercialProfileService commercialProfileService;

    public SpecialistCommercialProfileController(
        SpecialistCommercialProfileService commercialProfileService
    ) {
        this.commercialProfileService = commercialProfileService;
    }

    @GetMapping
    public SpecialistCommercialProfileResponse getProfile(
        Authentication authentication
    ) {
        Long specialistProfileId = getRequiredLongClaim(
            authentication,
            "specialistProfileId"
        );

        return commercialProfileService.getProfile(
            specialistProfileId
        );
    }

    @PutMapping
    public SpecialistCommercialProfileResponse updateProfile(
        Authentication authentication,
        @Valid @RequestBody UpdateSpecialistCommercialProfileRequest request
    ) {
        Long specialistProfileId = getRequiredLongClaim(
            authentication,
            "specialistProfileId"
        );

        return commercialProfileService.updateProfile(
            specialistProfileId,
            request
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
                "El token no contiene el claim requerido: "
                    + claimName
            );
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "El claim "
                    + claimName
                    + " no contiene un identificador valido."
            );
        }
    }
}