package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.ManualPaymentResponse;
import pe.prismadev.servmedic.service.PaymentService;

@RestController
@RequestMapping("/api/patient/medical-requests/{medicalRequestId}/payment")
public class PatientManualPaymentController {

    private final PaymentService paymentService;

    public PatientManualPaymentController(
        PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ManualPaymentResponse register(
        Authentication authentication,
        @PathVariable Long medicalRequestId,
        @RequestParam String paymentMethod,
        @RequestParam(required = false)
        String externalTransactionId,
        @RequestPart("evidence")
        MultipartFile evidence
    ) {
        return paymentService.registerForPatient(
            medicalRequestId,
            claim(authentication, "patientProfileId"),
            paymentMethod,
            externalTransactionId,
            evidence
        );
    }

    @GetMapping
    public ManualPaymentResponse find(
        Authentication authentication,
        @PathVariable Long medicalRequestId
    ) {
        return paymentService.findForPatient(
            medicalRequestId,
            claim(authentication, "patientProfileId")
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

        Object value = claims.get(name);

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Claim requerido ausente: " + name
            );
        }

        try {
            return Long.valueOf(value.toString());
        }
        catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Claim invalido: " + name
            );
        }
    }
}