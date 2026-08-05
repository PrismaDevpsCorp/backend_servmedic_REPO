package pe.prismadev.servmedic.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CreateMedicalRequestAdditionalRequest;
import pe.prismadev.servmedic.dto.MedicalRequestAdditionalResponse;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.MedicalRequestAdditional;
import pe.prismadev.servmedic.entity.MedicalRequestProposal;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalRequestAdditionalRepository;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class MedicalRequestAdditionalService {

    private static final BigDecimal MIN_AMOUNT =
        new BigDecimal("0.01");

    private static final BigDecimal MAX_AMOUNT =
        new BigDecimal("99999999.99");

    private final MedicalRequestRepository medicalRequestRepository;
    private final MedicalRequestAdditionalRepository additionalRepository;

    public MedicalRequestAdditionalService(
        MedicalRequestRepository medicalRequestRepository,
        MedicalRequestAdditionalRepository additionalRepository
    ) {
        this.medicalRequestRepository = medicalRequestRepository;
        this.additionalRepository = additionalRepository;
    }

    @Transactional
    public MedicalRequestAdditionalResponse createForSpecialist(
        Long medicalRequestId,
        Long specialistProfileId,
        CreateMedicalRequestAdditionalRequest request
    ) {
        validateRequiredId(
            medicalRequestId,
            "El identificador de la solicitud es obligatorio."
        );

        validateRequiredId(
            specialistProfileId,
            "No se pudo identificar al especialista autenticado."
        );

        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Los datos del adicional son obligatorios."
            );
        }

        MedicalRequest medicalRequest =
            findRequestForUpdate(medicalRequestId);

        validateRequestSupportsAdditionals(medicalRequest);
        validateAssignedSpecialist(
            medicalRequest,
            specialistProfileId
        );
        validateRequestInAttention(medicalRequest);

        String concept = cleanRequiredText(
            request.concept(),
            3,
            120,
            "El concepto del adicional"
        );

        String justification = cleanRequiredText(
            request.justification(),
            10,
            1000,
            "La justificacion del adicional"
        );

        BigDecimal amount = normalizeAmount(request.amount());

        MedicalRequestAdditional additional =
            new MedicalRequestAdditional(
                medicalRequest,
                medicalRequest.getAcceptedSpecialistProfile(),
                concept,
                justification,
                amount
            );

        try {
            MedicalRequestAdditional saved =
                additionalRepository.saveAndFlush(additional);

            return toResponse(saved);
        }
        catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se pudo registrar el adicional debido a un conflicto "
                    + "de integridad o de estado de la solicitud.",
                exception
            );
        }
    }

    @Transactional(readOnly = true)
    public List<MedicalRequestAdditionalResponse> listForSpecialist(
        Long medicalRequestId,
        Long specialistProfileId
    ) {
        validateRequiredId(
            medicalRequestId,
            "El identificador de la solicitud es obligatorio."
        );

        validateRequiredId(
            specialistProfileId,
            "No se pudo identificar al especialista autenticado."
        );

        MedicalRequest medicalRequest =
            findRequest(medicalRequestId);

        validateRequestSupportsAdditionals(medicalRequest);
        validateAssignedSpecialist(
            medicalRequest,
            specialistProfileId
        );

        List<MedicalRequestAdditional> additionals =
            additionalRepository
                .findDetailedByRequestIdAndSpecialistId(
                    medicalRequestId,
                    specialistProfileId
                );

        return mapList(
            medicalRequest,
            additionals
        );
    }

    @Transactional
    public MedicalRequestAdditionalResponse withdrawForSpecialist(
        Long medicalRequestId,
        Long additionalId,
        Long specialistProfileId
    ) {
        validateRequiredId(
            medicalRequestId,
            "El identificador de la solicitud es obligatorio."
        );

        validateRequiredId(
            additionalId,
            "El identificador del adicional es obligatorio."
        );

        validateRequiredId(
            specialistProfileId,
            "No se pudo identificar al especialista autenticado."
        );

        MedicalRequest medicalRequest =
            findRequestForUpdate(medicalRequestId);

        validateRequestSupportsAdditionals(medicalRequest);
        validateAssignedSpecialist(
            medicalRequest,
            specialistProfileId
        );
        validateRequestInAttention(medicalRequest);

        MedicalRequestAdditional additional =
            findAdditionalForUpdate(
                medicalRequestId,
                additionalId
            );

        Long ownerSpecialistId =
            additional.getSpecialistProfile().getId();

        if (!Objects.equals(
            ownerSpecialistId,
            specialistProfileId
        )) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El adicional no pertenece al especialista autenticado."
            );
        }

        try {
            additional.withdraw(OffsetDateTime.now());
            additionalRepository.flush();

            return toResponse(additional);
        }
        catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                exception
            );
        }
        catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                exception
            );
        }
        catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se pudo retirar el adicional debido a un conflicto "
                    + "de integridad o concurrencia.",
                exception
            );
        }
    }

    @Transactional(readOnly = true)
    public List<MedicalRequestAdditionalResponse> listForPatient(
        Long medicalRequestId,
        Long patientProfileId
    ) {
        validateRequiredId(
            medicalRequestId,
            "El identificador de la solicitud es obligatorio."
        );

        validateRequiredId(
            patientProfileId,
            "No se pudo identificar al pa.iente autenticado."
        );

        MedicalRequest medicalRequest =
            findRequest(medicalRequestId);

        validateRequestSupportsAdditionals(medicalRequest);
        validateOwnerPatient(
            medicalRequest,
            patientProfileId
        );

        List<MedicalRequestAdditional> additionals =
            additionalRepository.findDetailedByRequestId(
                medicalRequestId
            );

        return mapList(
            medicalRequest,
            additionals
        );
    }

    @Transactional
    public MedicalRequestAdditionalResponse approveForPatient(
        Long medicalRequestId,
        Long additionalId,
        Long patientProfileId
    ) {
        return respondForPatient(
            medicalRequestId,
            additionalId,
            patientProfileId,
            true
        );
    }

    @Transactional
    public MedicalRequestAdditionalResponse rejectForPatient(
        Long medicalRequestId,
        Long additionalId,
        Long patientProfileId
    ) {
        return respondForPatient(
            medicalRequestId,
            additionalId,
            patientProfileId,
            false
        );
    }

    private MedicalRequestAdditionalResponse respondForPatient(
        Long medicalRequestId,
        Long additionalId,
        Long patientProfileId,
        boolean approve
    ) {
        validateRequiredId(
            medicalRequestId,
            "El identificador de la solicitud es obligatorio."
        );

        validateRequiredId(
            additionalId,
            "El identificador del adicional es obligatorio."
        );

        validateRequiredId(
            patientProfileId,
            "No se pudo identificar al pa.iente autenticado."
        );

        MedicalRequest medicalRequest =
            findRequestForUpdate(medicalRequestId);

        validateRequestSupportsAdditionals(medicalRequest);
        validateOwnerPatient(
            medicalRequest,
            patientProfileId
        );
        validateRequestInAttention(medicalRequest);

        MedicalRequestAdditional additional =
            findAdditionalForUpdate(
                medicalRequestId,
                additionalId
            );

        OffsetDateTime responseTime = OffsetDateTime.now();

        try {
            if (approve) {
                additional.approve(responseTime);
            }
            else {
                additional.reject(responseTime);
            }

            additionalRepository.flush();

            return toResponse(additional);
        }
        catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                exception
            );
        }
        catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                exception
            );
        }
        catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se pudo registrar la decision debido a un conflicto "
                    + "de integridad o concurrencia.",
                exception
            );
        }
    }

    private MedicalRequest findRequest(Long medicalRequestId) {
        return medicalRequestRepository.findDetailedById(
            medicalRequestId
        )
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Solicitud medica no encontrada: " + medicalRequestId
        ));
    }

    private MedicalRequest findRequestForUpdate(
        Long medicalRequestId
    ) {
        return medicalRequestRepository.findDetailedByIdForUpdate(
            medicalRequestId
        )
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Solicitud medica no encontrada: " + medicalRequestId
        ));
    }

    private MedicalRequestAdditional findAdditionalForUpdate(
        Long medicalRequestId,
        Long additionalId
    ) {
        return additionalRepository
            .findDetailedByIdAndRequestIdForUpdate(
                additionalId,
                medicalRequestId
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Adicional no encontrado para la solicitud indicada."
            ));
    }

    private void validateRequestSupportsAdditionals(
        MedicalRequest medicalRequest
    ) {
        if (medicalRequest.getAcceptedProposal() == null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La solicitud no tiene una propuesta aceptada."
            );
        }

        if (
            medicalRequest.getAcceptedSpecialistProfile()
                == null
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La solicitud no tiene un especialista asignado."
            );
        }

        String status = medicalRequest.getStatus();

        if (
            !"ACCEPTED".equals(status)
                && !"EN_CAMINO".equals(status)
                && !"EN_ATENCION".equals(status)
                && !"FINALIZADO".equals(status)
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La solicitud no admite consulta de adicionales. "
                    + "Estado actual: " + status + "."
            );
        }
    }

    private void validateRequestInAttention(
        MedicalRequest medicalRequest
    ) {
        if (!"EN_ATENCION".equals(medicalRequest.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Los adicionales solo pueden gestionarse durante "
                    + "la atencion. Estado requerido: EN_ATENCION. "
                    + "Estado actual: "
                    + medicalRequest.getStatus() + "."
            );
        }
    }

    private void validateAssignedSpecialist(
        MedicalRequest medicalRequest,
        Long specialistProfileId
    ) {
        SpecialistProfile assignedSpecialist =
            medicalRequest.getAcceptedSpecialistProfile();

        if (
            assignedSpecialist == null
                || !Objects.equals(
                    assignedSpecialist.getId(),
                    specialistProfileId
                )
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo el especialista asignado puede gestionar "
                    + "adicionales de esta solicitud."
            );
        }
    }

    private void validateOwnerPatient(
        MedicalRequest medicalRequest,
        Long patientProfileId
    ) {
        Long ownerPatientId =
            medicalRequest.getPatientProfile().getId();

        if (!Objects.equals(
            ownerPatientId,
            patientProfileId
        )) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La solicitud medica no pertenece al pa.iente autenticado."
            );
        }
    }

    private void validateRequiredId(
        Long value,
        String errorMessage
    ) {
        if (value == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                errorMessage
            );
        }
    }

    private String cleanRequiredText(
        String value,
        int minimumLength,
        int maximumLength,
        String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + " es obligatorio."
            );
        }

        String cleaned = value.trim();

        if (
            cleaned.length() < minimumLength
                || cleaned.length() > maximumLength
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + " debe tener entre "
                    + minimumLength + " y "
                    + maximumLength + " caracteres."
            );
        }

        return cleaned;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (
            amount == null
                || amount.compareTo(MIN_AMOUNT) < 0
                || amount.compareTo(MAX_AMOUNT) > 0
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El importe del adicional debe estar entre S/ 0.01 "
                    + "y S/ 99999999.99."
            );
        }

        if (amount.scale() > 2) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El importe del adicional admite como maximo "
                    + "dos decimales."
            );
        }

        return amount.setScale(
            2,
            RoundingMode.HALF_UP
        );
    }

    private List<MedicalRequestAdditionalResponse> mapList(
        MedicalRequest medicalRequest,
        List<MedicalRequestAdditional> additionals
    ) {
        BigDecimal originalTotal =
            resolveOriginalTotal(medicalRequest);

        BigDecimal approvedAdditionals =
            resolveApprovedAdditionals(medicalRequest.getId());

        return additionals
            .stream()
            .map(additional -> toResponse(
                additional,
                originalTotal,
                approvedAdditionals
            ))
            .toList();
    }

    private MedicalRequestAdditionalResponse toResponse(
        MedicalRequestAdditional additional
    ) {
        MedicalRequest medicalRequest =
            additional.getMedicalRequest();

        BigDecimal originalTotal =
            resolveOriginalTotal(medicalRequest);

        BigDecimal approvedAdditionals =
            resolveApprovedAdditionals(medicalRequest.getId());

        return toResponse(
            additional,
            originalTotal,
            approvedAdditionals
        );
    }

    private MedicalRequestAdditionalResponse toResponse(
        MedicalRequestAdditional additional,
        BigDecimal originalTotal,
        BigDecimal approvedAdditionals
    ) {
        MedicalRequest medicalRequest =
            additional.getMedicalRequest();

        SpecialistProfile specialist =
            additional.getSpecialistProfile();

        UserAccount specialistUser =
            specialist.getUserAccount();

        BigDecimal currentTotal = originalTotal
            .add(approvedAdditionals)
            .setScale(2, RoundingMode.HALF_UP);

        return new MedicalRequestAdditionalResponse(
            additional.getId(),
            medicalRequest.getId(),
            medicalRequest.getRequestCode(),
            medicalRequest.getStatus(),
            specialist.getId(),
            buildFullName(specialistUser),
            additional.getConcept(),
            additional.getJustification(),
            additional.getAmount(),
            additional.getCurrency(),
            additional.getStatus(),
            originalTotal,
            approvedAdditionals,
            currentTotal,
            additional.getCreatedAt(),
            additional.getUpdatedAt(),
            additional.getRespondedAt(),
            additional.getWithdrawnAt()
        );
    }

    private BigDecimal resolveOriginalTotal(
        MedicalRequest medicalRequest
    ) {
        MedicalRequestProposal acceptedProposal =
            medicalRequest.getAcceptedProposal();

        if (
            acceptedProposal == null
                || acceptedProposal.getTotalAmount() == null
                || acceptedProposal.getTotalAmount().signum() <= 0
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La solicitud no tiene un total original aceptado valido."
            );
        }

        return acceptedProposal
            .getTotalAmount()
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveApprovedAdditionals(
        Long medicalRequestId
    ) {
        BigDecimal total =
            additionalRepository.sumApprovedAmountByRequestId(
                medicalRequestId
            );

        if (total == null) {
            return BigDecimal.ZERO.setScale(
                2,
                RoundingMode.HALF_UP
            );
        }

        return total.setScale(
            2,
            RoundingMode.HALF_UP
        );
    }

    private String buildFullName(
        UserAccount userAccount
    ) {
        String firstName =
            userAccount.getFirstName() == null
                ? ""
                : userAccount.getFirstName().trim();

        String lastName =
            userAccount.getLastName() == null
                ? ""
                : userAccount.getLastName().trim();

        return (firstName + " " + lastName).trim();
    }
}
