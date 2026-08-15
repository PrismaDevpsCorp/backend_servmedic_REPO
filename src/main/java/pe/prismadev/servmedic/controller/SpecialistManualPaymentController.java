package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.ConfirmManualPaymentRequest;
import pe.prismadev.servmedic.dto.ManualPaymentResponse;
import pe.prismadev.servmedic.dto.RejectManualPaymentRequest;
import pe.prismadev.servmedic.entity.MedicalPayment;
import pe.prismadev.servmedic.service.PaymentService;

@RestController
@RequestMapping("/api/specialist/medical-requests/{medicalRequestId}/payment")
public class SpecialistManualPaymentController {

    private final PaymentService paymentService;

    public SpecialistManualPaymentController(
        PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ManualPaymentResponse find(
        Authentication authentication,
        @PathVariable Long medicalRequestId
    ) {
        return paymentService.findForSpecialist(
            medicalRequestId,
            claim(authentication, "specialistProfileId")
        );
    }

    @GetMapping("/evidence")
    public ResponseEntity<byte[]> evidence(
        Authentication authentication,
        @PathVariable Long medicalRequestId
    ) {
        MedicalPayment payment =
            paymentService.getEvidenceForSpecialist(
                medicalRequestId,
                claim(authentication, "specialistProfileId")
            );

        MediaType type;

        try {
            type = MediaType.parseMediaType(
                payment.getEvidenceContentType()
            );
        }
        catch (Exception exception) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
            .contentType(type)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" +
                    payment.getEvidenceFileName() +
                    "\""
            )
            .contentLength(payment.getEvidenceSize())
            .body(payment.getEvidenceData());
    }

    @PatchMapping("/confirm")
    public ManualPaymentResponse confirm(
        Authentication authentication,
        @PathVariable Long medicalRequestId,
        @Valid @RequestBody
        ConfirmManualPaymentRequest request
    ) {
        return paymentService.confirmForSpecialist(
            medicalRequestId,
            claim(authentication, "specialistProfileId"),
            request.warningAcknowledged()
        );
    }

    @PatchMapping("/reject")
    public ManualPaymentResponse reject(
        Authentication authentication,
        @PathVariable Long medicalRequestId,
        @Valid @RequestBody
        RejectManualPaymentRequest request
    ) {
        return paymentService.rejectForSpecialist(
            medicalRequestId,
            claim(
                authentication,
                "specialistProfileId"
            ),
            request.reason()
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