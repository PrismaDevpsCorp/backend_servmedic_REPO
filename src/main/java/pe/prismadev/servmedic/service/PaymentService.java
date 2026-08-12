package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.ManualPaymentResponse;
import pe.prismadev.servmedic.entity.*;
import pe.prismadev.servmedic.repository.MedicalPaymentRepository;
import pe.prismadev.servmedic.repository.MedicalRequestAdditionalRepository;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class PaymentService {

    private static final BigDecimal COMMISSION_PERCENT = new BigDecimal("5.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private static final long MAX_EVIDENCE_SIZE = 5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    private final MedicalRequestRepository medicalRequestRepository;
    private final MedicalPaymentRepository medicalPaymentRepository;
    private final MedicalRequestAdditionalRepository additionalRepository;

    public PaymentService(
        MedicalRequestRepository medicalRequestRepository,
        MedicalPaymentRepository medicalPaymentRepository,
        MedicalRequestAdditionalRepository additionalRepository
    ) {
        this.medicalRequestRepository = medicalRequestRepository;
        this.medicalPaymentRepository = medicalPaymentRepository;
        this.additionalRepository = additionalRepository;
    }

    @Transactional
    public ManualPaymentResponse registerForPatient(
        Long medicalRequestId,
        Long patientProfileId,
        String paymentMethod,
        String externalTransactionId,
        MultipartFile evidence
    ) {
        MedicalRequest request = medicalRequestRepository
            .findDetailedByIdForUpdate(medicalRequestId)
            .orElseThrow(() -> notFound(
                "Solicitud medica no encontrada: " + medicalRequestId
            ));

        validatePatient(request, patientProfileId);
        validatePayableRequest(request);

        if (medicalPaymentRepository.existsByMedicalRequestId(medicalRequestId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La solicitud ya tiene un pago registrado."
            );
        }

        MedicalRequestProposal proposal = request.getAcceptedProposal();

        if (proposal == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud no tiene propuesta aceptada."
            );
        }

        String normalizedMethod = normalizePaymentMethod(paymentMethod);
        validateEvidence(evidence);

        BigDecimal serviceAmount = money(proposal.getServiceAmount());
        BigDecimal mobilityAmount = money(proposal.getMobilityAmount());

        BigDecimal additionalAmount = money(
            additionalRepository.sumApprovedAmountByRequestId(
                medicalRequestId
            )
        );

        BigDecimal totalAmount = serviceAmount
            .add(mobilityAmount)
            .add(additionalAmount)
            .setScale(2, RoundingMode.HALF_UP);

        MedicalPayment payment = new MedicalPayment();

        payment.setMedicalRequest(request);
        payment.setPatientProfile(request.getPatientProfile());
        payment.setSpecialistProfile(
            request.getAcceptedSpecialistProfile()
        );

        payment.setServiceAmount(serviceAmount);
        payment.setMobilityAmount(mobilityAmount);
        payment.setAdditionalAmount(additionalAmount);
        payment.setAmount(totalAmount);

        payment.setPlatformCommissionPercent(COMMISSION_PERCENT);
        payment.setPlatformCommissionAmount(BigDecimal.ZERO.setScale(2));
        payment.setSpecialistNetAmount(totalAmount);

        payment.setCurrency("PEN");
        payment.setPaymentFlow("DIRECT_EXTERNAL");
        payment.setPaymentMethod(normalizedMethod);
        payment.setStatus("PENDING");

        payment.setExternalTransactionId(
            cleanNullable(externalTransactionId)
        );

        payment.setEvidenceFileName(
            safeFileName(evidence.getOriginalFilename())
        );

        payment.setEvidenceContentType(
            normalizeContentType(evidence.getContentType())
        );

        payment.setEvidenceSize(evidence.getSize());

        try {
            payment.setEvidenceData(evidence.getBytes());
        }
        catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se pudo leer la evidencia de pago.",
                exception
            );
        }

        payment.setEvidenceUploadedAt(OffsetDateTime.now());

        MedicalPayment saved = medicalPaymentRepository.save(payment);

        return toResponse(
            saved,
            "Evidencia registrada. Pendiente de verificacion por el especialista."
        );
    }

    @Transactional(readOnly = true)
    public ManualPaymentResponse findForPatient(
        Long medicalRequestId,
        Long patientProfileId
    ) {
        MedicalPayment payment = findPayment(medicalRequestId);

        validatePatient(
            payment.getMedicalRequest(),
            patientProfileId
        );

        return toResponse(payment, "Pago encontrado.");
    }

    @Transactional(readOnly = true)
    public ManualPaymentResponse findForSpecialist(
        Long medicalRequestId,
        Long specialistProfileId
    ) {
        MedicalPayment payment = findPayment(medicalRequestId);

        validateSpecialist(payment, specialistProfileId);

        return toResponse(payment, "Pago encontrado.");
    }

    @Transactional(readOnly = true)
    public MedicalPayment getEvidenceForSpecialist(
        Long medicalRequestId,
        Long specialistProfileId
    ) {
        MedicalPayment payment = findPayment(medicalRequestId);

        validateSpecialist(payment, specialistProfileId);

        if (payment.getEvidenceData() == null) {
            throw notFound("La evidencia de pago no existe.");
        }

        return payment;
    }

    @Transactional
    public ManualPaymentResponse confirmForSpecialist(
        Long medicalRequestId,
        Long specialistProfileId,
        boolean warningAcknowledged
    ) {
        if (!warningAcknowledged) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debe confirmar que verifico correctamente el pago."
            );
        }

        MedicalPayment payment = medicalPaymentRepository
            .findDetailedByMedicalRequestIdForUpdate(medicalRequestId)
            .orElseThrow(() -> notFound(
                "Pago no encontrado para la solicitud: "
                    + medicalRequestId
            ));

        validateSpecialist(payment, specialistProfileId);

        if (!"DIRECT_EXTERNAL".equals(payment.getPaymentFlow())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El pago no pertenece al flujo manual directo."
            );
        }

        if (payment.isPaid()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El pago ya fue confirmado."
            );
        }

        if (!payment.isPending()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El pago no esta pendiente."
            );
        }

        BigDecimal commission = payment.getServiceAmount()
            .multiply(COMMISSION_PERCENT)
            .divide(
                ONE_HUNDRED,
                2,
                RoundingMode.HALF_UP
            );

        BigDecimal specialistNet = payment.getAmount()
            .subtract(commission)
            .setScale(2, RoundingMode.HALF_UP);

        OffsetDateTime now = OffsetDateTime.now();

        payment.setPlatformCommissionPercent(COMMISSION_PERCENT);
        payment.setPlatformCommissionAmount(commission);
        payment.setSpecialistNetAmount(specialistNet);

        payment.setVerifiedBySpecialistProfile(
            payment.getSpecialistProfile()
        );

        payment.setVerificationWarningAcknowledged(true);
        payment.setVerifiedAt(now);
        payment.setPaidAt(now);
        payment.setStatus("PAID");

        MedicalPayment saved = medicalPaymentRepository.save(payment);

        return toResponse(
            saved,
            "Pago verificado y confirmado por el especialista."
        );
    }

    private MedicalPayment findPayment(Long medicalRequestId) {
        return medicalPaymentRepository
            .findDetailedByMedicalRequestId(medicalRequestId)
            .orElseThrow(() -> notFound(
                "Pago no encontrado para la solicitud: "
                    + medicalRequestId
            ));
    }

    private void validatePayableRequest(MedicalRequest request) {
        if (!"FINALIZADO".equals(request.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se puede registrar el pago de una solicitud FINALIZADA."
            );
        }

        if (request.getAcceptedSpecialistProfile() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud no tiene especialista asignado."
            );
        }

        if (request.getAcceptedProposal() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud no tiene propuesta aceptada."
            );
        }

        if (
            additionalRepository.existsByMedicalRequestIdAndStatus(
                request.getId(),
                "PENDING"
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La solicitud tiene adicionales pendientes."
            );
        }
    }

    private void validatePatient(
        MedicalRequest request,
        Long patientProfileId
    ) {
        if (
            request.getPatientProfile() == null
                || !request.getPatientProfile().getId()
                    .equals(patientProfileId)
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La solicitud no pertenece al paciente autenticado."
            );
        }
    }

    private void validateSpecialist(
        MedicalPayment payment,
        Long specialistProfileId
    ) {
        if (
            payment.getSpecialistProfile() == null
                || !payment.getSpecialistProfile().getId()
                    .equals(specialistProfileId)
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El pago no pertenece al especialista autenticado."
            );
        }
    }

    private void validateEvidence(MultipartFile evidence) {
        if (evidence == null || evidence.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La evidencia de pago es obligatoria."
            );
        }

        if (evidence.getSize() > MAX_EVIDENCE_SIZE) {
            throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "La evidencia supera el limite de 5 MB."
            );
        }

        String contentType = normalizeContentType(
            evidence.getContentType()
        );

        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Formato no permitido. Use JPG, PNG o WEBP."
            );
        }
    }

    private String normalizePaymentMethod(String value) {
        String method = value == null
            ? ""
            : value.trim().toUpperCase(Locale.ROOT);

        return switch (method) {
            case "YAPE", "PLIN", "TRANSFER", "CASH" -> method;
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Metodo de pago no permitido: " + value
            );
        };
    }

    private String normalizeContentType(String value) {
        return value == null
            ? ""
            : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "evidencia";
        }

        String fileName = value
            .replace("\\", "_")
            .replace("/", "_")
            .trim();

        if (fileName.length() > 255) {
            return fileName.substring(0, 255);
        }

        return fileName;
    }

    private BigDecimal money(BigDecimal value) {
        return (
            value == null
                ? BigDecimal.ZERO
                : value
        ).setScale(
            2,
            RoundingMode.HALF_UP
        );
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank()
            ? null
            : value.trim();
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            message
        );
    }

    private ManualPaymentResponse toResponse(
        MedicalPayment payment,
        String message
    ) {
        MedicalRequest request = payment.getMedicalRequest();

        UserAccount patientUser =
            payment.getPatientProfile().getUserAccount();

        UserAccount specialistUser =
            payment.getSpecialistProfile().getUserAccount();

        return new ManualPaymentResponse(
            payment.getId(),
            request.getId(),
            request.getRequestCode(),
            payment.getPaymentFlow(),
            payment.getStatus(),
            payment.getPaymentMethod(),
            payment.getServiceAmount(),
            payment.getMobilityAmount(),
            payment.getAdditionalAmount(),
            payment.getAmount(),
            payment.getServiceAmount(),
            payment.getPlatformCommissionPercent(),
            payment.getPlatformCommissionAmount(),
            payment.getSpecialistNetAmount(),
            payment.getCurrency(),
            payment.getPatientProfile().getId(),
            patientUser.getFirstName()
                + " "
                + patientUser.getLastName(),
            payment.getSpecialistProfile().getId(),
            specialistUser.getFirstName()
                + " "
                + specialistUser.getLastName(),
            payment.getExternalTransactionId(),
            payment.getEvidenceData() != null,
            payment.getEvidenceFileName(),
            payment.getEvidenceContentType(),
            payment.getEvidenceSize(),
            payment.getEvidenceUploadedAt(),
            payment.getPaidAt(),
            payment.getVerifiedAt(),
            payment.isVerificationWarningAcknowledged(),
            message
        );
    }
}