package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.SpecialistFinancialDashboardResponse;
import pe.prismadev.servmedic.service.SpecialistFinancialDashboardService;

@RestController
@RequestMapping("/api/specialist/financial-dashboard")
public class SpecialistFinancialDashboardController {

    private final SpecialistFinancialDashboardService
        dashboardService;

    public SpecialistFinancialDashboardController(
        SpecialistFinancialDashboardService dashboardService
    ) {
        this.dashboardService =
            dashboardService;
    }

    @GetMapping
    public SpecialistFinancialDashboardResponse find(
        Authentication authentication,
        @RequestParam(
            defaultValue = "0"
        )
        int page,
        @RequestParam(
            defaultValue = "20"
        )
        int size
    ) {
        return dashboardService.find(
            claim(
                authentication,
                "specialistProfileId"
            ),
            page,
            size
        );
    }

    private Long claim(
        Authentication authentication,
        String name
    ) {
        if (
            authentication == null
                || !(authentication.getDetails()
                    instanceof Claims claims)
        ) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token no valido o sin claims."
            );
        }

        Object value =
            claims.get(name);

        if (
            value instanceof Number number
        ) {
            return number.longValue();
        }

        if (value == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Claim requerido ausente: "
                    + name
            );
        }

        try {
            return Long.valueOf(
                value.toString()
            );
        }
        catch (
            NumberFormatException exception
        ) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Claim invalido: "
                    + name
            );
        }
    }
}
