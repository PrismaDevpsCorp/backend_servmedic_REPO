package pe.prismadev.servmedic.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.PayMedicalRequestRequest;
import pe.prismadev.servmedic.dto.PaymentResponse;
import pe.prismadev.servmedic.service.PaymentService;

@RestController
@RequestMapping("/api/public/medical-requests")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{medicalRequestId}/payment")
    public PaymentResponse pay(
        @PathVariable Long medicalRequestId,
        @Valid @RequestBody PayMedicalRequestRequest request
    ) {
        return paymentService.pay(medicalRequestId, request);
    }

    @GetMapping("/{medicalRequestId}/payment")
    public PaymentResponse findPayment(
        @PathVariable Long medicalRequestId
    ) {
        return paymentService.findByMedicalRequestId(medicalRequestId);
    }
}