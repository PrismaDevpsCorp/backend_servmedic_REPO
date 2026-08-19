package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.SpecialistPaymentSettingResponse;
import pe.prismadev.servmedic.dto.UpdateSpecialistPaymentSettingRequest;
import pe.prismadev.servmedic.service.SpecialistPaymentSettingsService;

import java.util.List;

@RestController
@RequestMapping(
    "/api/specialist/commercial-profile/payment-settings"
)
public class SpecialistPaymentSettingsController {

    private final SpecialistPaymentSettingsService service;

    public SpecialistPaymentSettingsController(
        SpecialistPaymentSettingsService service
    ) {
        this.service = service;
    }

    @GetMapping
    public List<SpecialistPaymentSettingResponse> getSettings(
        Authentication authentication
    ) {
        Long specialistProfileId =
            getRequiredLongClaim(
                authentication,
                "specialistProfileId"
            );

        return service.getSettings(
            specialistProfileId
        );
    }

    @PutMapping
    public List<SpecialistPaymentSettingResponse> updateSettings(
        Authentication authentication,
        @RequestBody
        List<UpdateSpecialistPaymentSettingRequest> request
    ) {
        Long specialistProfileId =
            getRequiredLongClaim(
                authentication,
                "specialistProfileId"
            );

        return service.updateSettings(
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
                || !(
                    authentication.getDetails()
                        instanceof Claims claims
                )
        ) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token no valido o sin claims."
            );
        }

        Object value =
            claims.get(
                claimName
            );

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

            return Long.valueOf(
                value.toString()
            );
        }
        catch (NumberFormatException exception) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Claim invalido: "
                    + claimName
            );
        }
    }
}
