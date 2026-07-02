package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.PayMedicalRequestRequest;
import pe.prismadev.servmedic.dto.PaymentResponse;
import pe.prismadev.servmedic.entity.*;
import pe.prismadev.servmedic.repository.MedicalPaymentRepository;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;
import pe.prismadev.servmedic.repository.SpecialistWalletTransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PaymentService {

    private static final BigDecimal PLATFORM_COMMISSION_PERCENT = new BigDecimal("20.00");

    private final MedicalRequestRepository medicalRequestRepository;
    private final MedicalPaymentRepository medicalPaymentRepository;
    private final SpecialistWalletTransactionRepository walletTransactionRepository;

    public PaymentService(
        MedicalRequestRepository medicalRequestRepository,
        MedicalPaymentRepository medicalPaymentRepository,
        SpecialistWalletTransactionRepository walletTransactionRepository
    ) {
        this.medicalRequestRepository = medicalRequestRepository;
        this.medicalPaymentRepository = medicalPaymentRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional
    public PaymentResponse pay(Long medicalRequestId, PayMedicalRequestRequest request) {
        MedicalRequest medicalRequest = medicalRequestRepository.findDetailedByIdForUpdate(medicalRequestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + medicalRequestId
            ));

        validatePayableRequest(medicalRequest);

        if (medicalPaymentRepository.existsByMedicalRequestId(medicalRequestId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La solicitud ya tiene un pago registrado."
            );
        }

        BigDecimal amount = medicalRequest.getEstimatedAmount();
        BigDecimal commissionAmount = amount
            .multiply(PLATFORM_COMMISSION_PERCENT)
            .divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);

        BigDecimal specialistNetAmount = amount.subtract(commissionAmount).setScale(2, RoundingMode.HALF_UP);

        MedicalPayment payment = new MedicalPayment();
        payment.setMedicalRequest(medicalRequest);
        payment.setPatientProfile(medicalRequest.getPatientProfile());
        payment.setSpecialistProfile(medicalRequest.getAcceptedSpecialistProfile());
        payment.setAmount(amount);
        payment.setPlatformCommissionPercent(PLATFORM_COMMISSION_PERCENT);
        payment.setPlatformCommissionAmount(commissionAmount);
        payment.setSpecialistNetAmount(specialistNetAmount);
        payment.setCurrency("PEN");
        payment.setPaymentMethod(normalizePaymentMethod(request.paymentMethod()));
        payment.setExternalTransactionId(cleanNullable(request.externalTransactionId()));

        MedicalPayment savedPayment = medicalPaymentRepository.save(payment);

        SpecialistWalletTransaction walletTransaction = new SpecialistWalletTransaction(
            savedPayment.getSpecialistProfile(),
            savedPayment,
            savedPayment.getSpecialistNetAmount(),
            "Pago por servicio " + medicalRequest.getRequestCode()
        );

        walletTransactionRepository.save(walletTransaction);

        MedicalPayment detailedPayment = medicalPaymentRepository.findDetailedByMedicalRequestId(medicalRequestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo recuperar el pago registrado."
            ));

        return toResponse(detailedPayment, "Pago registrado correctamente en modo simulado.");
    }

    @Transactional(readOnly = true)
    public PaymentResponse findByMedicalRequestId(Long medicalRequestId) {
        MedicalPayment payment = medicalPaymentRepository.findDetailedByMedicalRequestId(medicalRequestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pago no encontrado para la solicitud: " + medicalRequestId
            ));

        return toResponse(payment, "Pago encontrado.");
    }

    private void validatePayableRequest(MedicalRequest medicalRequest) {
        if (!"FINALIZADO".equals(medicalRequest.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se puede pagar una solicitud FINALIZADA. Estado actual: " + medicalRequest.getStatus()
            );
        }

        if (medicalRequest.getAcceptedSpecialistProfile() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud no tiene especialista asignado."
            );
        }
    }

    private PaymentResponse toResponse(MedicalPayment payment, String message) {
        MedicalRequest request = payment.getMedicalRequest();
        UserAccount patientUser = payment.getPatientProfile().getUserAccount();
        UserAccount specialistUser = payment.getSpecialistProfile().getUserAccount();

        BigDecimal availableBalance = walletTransactionRepository.getAvailableBalance(payment.getSpecialistProfile().getId());

        return new PaymentResponse(
            payment.getId(),
            request.getId(),
            request.getRequestCode(),
            payment.getStatus(),
            payment.getPaymentMethod(),
            payment.getAmount(),
            payment.getPlatformCommissionPercent(),
            payment.getPlatformCommissionAmount(),
            payment.getSpecialistNetAmount(),
            payment.getCurrency(),
            payment.getPatientProfile().getId(),
            patientUser.getFirstName() + " " + patientUser.getLastName(),
            payment.getSpecialistProfile().getId(),
            specialistUser.getFirstName() + " " + specialistUser.getLastName(),
            payment.getExternalTransactionId(),
            payment.getPaidAt(),
            availableBalance,
            message
        );
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase();

        return switch (normalized) {
            case "SIMULATED", "CARD", "YAPE", "PLIN", "TRANSFER", "CASH" -> normalized;
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Metodo de pago no permitido: " + paymentMethod
            );
        };
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}