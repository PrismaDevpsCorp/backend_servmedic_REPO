package pe.prismadev.servmedic.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CreateMedicalRequestProposalRequest;
import pe.prismadev.servmedic.dto.MedicalRequestProposalResponse;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.MedicalRequestProposal;
import pe.prismadev.servmedic.entity.MedicalService;
import pe.prismadev.servmedic.entity.Profession;
import pe.prismadev.servmedic.entity.SpecialistCommercialProfile;
import pe.prismadev.servmedic.entity.SpecialistOfferedService;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalRequestProposalRepository;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;
import pe.prismadev.servmedic.repository.SpecialistCommercialProfileRepository;
import pe.prismadev.servmedic.repository.SpecialistOfferedServiceRepository;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class MedicalRequestProposalService {

    private static final int DEFAULT_VALIDITY_MINUTES = 30;
    private static final int MIN_VALIDITY_MINUTES = 5;
    private static final int MAX_VALIDITY_MINUTES = 120;
    private static final int MIN_ARRIVAL_MINUTES = 1;
    private static final int MAX_ARRIVAL_MINUTES = 1440;

    private final MedicalRequestRepository medicalRequestRepository;
    private final MedicalRequestProposalRepository proposalRepository;
    private final SpecialistProfileRepository specialistProfileRepository;
    private final SpecialistOfferedServiceRepository offeredServiceRepository;
    private final SpecialistCommercialProfileRepository commercialProfileRepository;

    public MedicalRequestProposalService(
        MedicalRequestRepository medicalRequestRepository,
        MedicalRequestProposalRepository proposalRepository,
        SpecialistProfileRepository specialistProfileRepository,
        SpecialistOfferedServiceRepository offeredServiceRepository,
        SpecialistCommercialProfileRepository commercialProfileRepository
    ) {
        this.medicalRequestRepository = medicalRequestRepository;
        this.proposalRepository = proposalRepository;
        this.specialistProfileRepository = specialistProfileRepository;
        this.offeredServiceRepository = offeredServiceRepository;
        this.commercialProfileRepository = commercialProfileRepository;
    }

    @Transactional
    public MedicalRequestProposalResponse createProposal(
        Long medicalRequestId,
        Long specialistProfileId,
        CreateMedicalRequestProposalRequest request
    ) {
        validateCreateArguments(
            medicalRequestId,
            specialistProfileId,
            request
        );

        MedicalRequest medicalRequest =
            medicalRequestRepository.findDetailedByIdForUpdate(
                medicalRequestId
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + medicalRequestId
            ));

        validateRequestCanReceiveProposal(medicalRequest);

        SpecialistProfile specialist =
            specialistProfileRepository.findDetailedById(
                specialistProfileId
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de especialista no encontrado: "
                    + specialistProfileId
            ));

        validateSpecialistCanPropose(
            medicalRequest,
            specialist
        );

        OffsetDateTime now = OffsetDateTime.now();

        MedicalRequestProposal existingPendingProposal =
            proposalRepository
                .findPendingByRequestAndSpecialistForUpdate(
                    medicalRequestId,
                    specialistProfileId
                )
                .orElse(null);

        if (existingPendingProposal != null) {
            if (!existingPendingProposal.isExpiredAt(now)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El especialista ya tiene una propuesta pendiente "
                        + "para esta solicitud."
                );
            }

            existingPendingProposal.expire(now);

            /*
             * La actualización debe llegar a PostgreSQL antes del INSERT
             * de la nueva propuesta, porque existe un índice único parcial
             * para propuestas PENDING por solicitud y especialista.
             */
            proposalRepository.flush();
        }

        MedicalService medicalService =
            medicalRequest.getMedicalService();

        SpecialistOfferedService offeredService =
            offeredServiceRepository
                .findActiveDetailedBySpecialistProfileIdAndMedicalServiceId(
                    specialistProfileId,
                    medicalService.getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El especialista no ofrece actualmente el servicio "
                        + medicalService.getCode() + "."
                ));

        BigDecimal serviceAmount = requirePositiveMoney(
            offeredService.getBasePrice(),
            "El servicio ofrecido no tiene un precio base valido."
        );

        SpecialistCommercialProfile commercialProfile =
            commercialProfileRepository
                .findDetailedBySpecialistProfileId(
                    specialistProfileId
                )
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El especialista no tiene un perfil comercial configurado."
                ));

        validateCommercialProfile(commercialProfile);

        String mobilityPolicy = normalizeMobilityPolicy(
            commercialProfile.getMobilityPolicy()
        );

        BigDecimal mobilityAmount = resolveMobilityAmount(
            commercialProfile,
            mobilityPolicy
        );

        BigDecimal totalAmount = serviceAmount
            .add(mobilityAmount)
            .setScale(2, RoundingMode.HALF_UP);

        int validityMinutes = resolveValidityMinutes(
            request.validityMinutes()
        );

        String proposalMessage = cleanAndValidateMessage(
            request.message()
        );

        OffsetDateTime expiresAt = now.plusMinutes(
            validityMinutes
        );

        MedicalRequestProposal proposal =
            new MedicalRequestProposal(
                medicalRequest,
                specialist,
                serviceAmount,
                mobilityPolicy,
                mobilityAmount,
                totalAmount,
                proposalMessage,
                request.estimatedArrivalMinutes(),
                expiresAt
            );

        try {
            MedicalRequestProposal savedProposal =
                proposalRepository.saveAndFlush(proposal);

            return toResponse(savedProposal);
        }
        catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se pudo registrar la propuesta. Verifique que no "
                    + "exista otra propuesta pendiente del especialista.",
                exception
            );
        }
    }

    @Transactional
    public MedicalRequestProposalResponse acceptForPatient(
        Long medicalRequestId,
        Long proposalId,
        Long patientProfileId
    ) {
        validateRequiredId(
            medicalRequestId,
            "El identificador de la solicitud es obligatorio."
        );

        validateRequiredId(
            proposalId,
            "El identificador de la propuesta es obligatorio."
        );

        validateRequiredId(
            patientProfileId,
            "No se pudo identificar al paciente autenticado."
        );

        MedicalRequest medicalRequest =
            medicalRequestRepository.findDetailedByIdForUpdate(
                medicalRequestId
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + medicalRequestId
            ));

        Long ownerPatientProfileId =
            medicalRequest.getPatientProfile().getId();

        if (!ownerPatientProfileId.equals(patientProfileId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La solicitud medica no pertenece al paciente autenticado."
            );
        }

        validateRequestCanReceiveProposal(medicalRequest);

        MedicalRequestProposal selectedProposal =
            proposalRepository
                .findDetailedByIdAndRequestIdForUpdate(
                    proposalId,
                    medicalRequestId
                )
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Propuesta no encontrada para la solicitud indicada."
                ));

        OffsetDateTime now = OffsetDateTime.now();

        if (!selectedProposal.isPending()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La propuesta ya no puede ser aceptada. Estado actual: "
                    + selectedProposal.getStatus() + "."
            );
        }

        if (selectedProposal.isExpiredAt(now)) {
            throw new ResponseStatusException(
                HttpStatus.GONE,
                "La propuesta ya vencio y no puede ser aceptada."
            );
        }

        List<MedicalRequestProposal> pendingProposals =
            proposalRepository.findPendingByRequestIdForUpdate(
                medicalRequestId
            );

        boolean selectedProposalIsLocked =
            pendingProposals
                .stream()
                .anyMatch(proposal ->
                    Objects.equals(
                        proposal.getId(),
                        selectedProposal.getId()
                    )
                );

        if (!selectedProposalIsLocked) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La propuesta seleccionada ya no se encuentra pendiente."
            );
        }

        try {
            medicalRequest.acceptProposal(
                selectedProposal,
                now
            );

            for (
                MedicalRequestProposal proposal
                : pendingProposals
            ) {
                if (
                    !Objects.equals(
                        proposal.getId(),
                        selectedProposal.getId()
                    )
                ) {
                    proposal.reject(now);
                }
            }

            /*
             * El flush persiste en una sola transaccion:
             * - propuesta seleccionada ACCEPTED;
             * - propuestas restantes REJECTED;
             * - solicitud ACCEPTED;
             * - especialista y total aceptados.
             */
            proposalRepository.flush();

            return toResponse(selectedProposal);
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
                "La solicitud ya fue aceptada o existe un conflicto "
                    + "de integridad con la propuesta.",
                exception
            );
        }
    }

    @Transactional
    public MedicalRequestProposalResponse withdrawForSpecialist(
        Long medicalRequestId,
        Long proposalId,
        Long specialistProfileId
    ) {
        validateRequiredId(
            medicalRequestId,
            "El identificador de la solicitud es obligatorio."
        );

        validateRequiredId(
            proposalId,
            "El identificador de la propuesta es obligatorio."
        );

        validateRequiredId(
            specialistProfileId,
            "No se pudo identificar al especialista autenticado."
        );

        MedicalRequest medicalRequest =
            medicalRequestRepository.findDetailedByIdForUpdate(
                medicalRequestId
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + medicalRequestId
            ));

        validateRequestCanReceiveProposal(medicalRequest);

        MedicalRequestProposal proposal =
            proposalRepository
                .findDetailedByIdAndRequestIdForUpdate(
                    proposalId,
                    medicalRequestId
                )
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Propuesta no encontrada para la solicitud indicada."
                ));

        Long ownerSpecialistProfileId =
            proposal.getSpecialistProfile().getId();

        if (!ownerSpecialistProfileId.equals(specialistProfileId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La propuesta no pertenece al especialista autenticado."
            );
        }

        if (!proposal.isPending()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La propuesta ya no puede ser retirada. Estado actual: "
                    + proposal.getStatus() + "."
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (proposal.isExpiredAt(now)) {
            proposal.expire(now);
            proposalRepository.flush();

            return toResponse(proposal);
        }

        try {
            proposal.withdraw(now);
            proposalRepository.flush();

            return toResponse(proposal);
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
                "No se pudo retirar la propuesta debido a un conflicto "
                    + "de integridad.",
                exception
            );
        }
    }

    @Transactional
    public List<MedicalRequestProposalResponse> listForPatient(
        Long medicalRequestId,
        Long patientProfileId
    ) {
        validateRequiredId(
            medicalRequestId,
            "El identificador de la solicitud es obligatorio."
        );

        validateRequiredId(
            patientProfileId,
            "No se pudo identificar al paciente autenticado."
        );

        MedicalRequest medicalRequest =
            medicalRequestRepository.findDetailedByIdForUpdate(
                medicalRequestId
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + medicalRequestId
            ));

        Long ownerPatientProfileId =
            medicalRequest.getPatientProfile().getId();

        if (!ownerPatientProfileId.equals(patientProfileId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La solicitud medica no pertenece al paciente autenticado."
            );
        }

        List<MedicalRequestProposal> proposals =
            proposalRepository.findDetailedByRequestId(
                medicalRequestId
            );

        expirePendingProposals(
            proposals,
            OffsetDateTime.now()
        );

        proposalRepository.flush();

        return proposals
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public List<MedicalRequestProposalResponse> listForSpecialist(
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

        medicalRequestRepository.findDetailedByIdForUpdate(
            medicalRequestId
        )
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Solicitud medica no encontrada: " + medicalRequestId
        ));

        specialistProfileRepository.findDetailedById(
            specialistProfileId
        )
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Perfil de especialista no encontrado: "
                + specialistProfileId
        ));

        List<MedicalRequestProposal> proposals =
            proposalRepository
                .findDetailedByRequestIdAndSpecialistId(
                    medicalRequestId,
                    specialistProfileId
                );

        expirePendingProposals(
            proposals,
            OffsetDateTime.now()
        );

        proposalRepository.flush();

        return proposals
            .stream()
            .map(this::toResponse)
            .toList();
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

    private void expirePendingProposals(
        List<MedicalRequestProposal> proposals,
        OffsetDateTime evaluationTime
    ) {
        for (MedicalRequestProposal proposal : proposals) {
            if (
                proposal.isPending()
                    && proposal.isExpiredAt(evaluationTime)
            ) {
                proposal.expire(evaluationTime);
            }
        }
    }

    private void validateCreateArguments(
        Long medicalRequestId,
        Long specialistProfileId,
        CreateMedicalRequestProposalRequest request
    ) {
        if (medicalRequestId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El identificador de la solicitud es obligatorio."
            );
        }

        if (specialistProfileId == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se pudo identificar al especialista autenticado."
            );
        }

        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Los datos de la propuesta son obligatorios."
            );
        }

        Integer arrivalMinutes =
            request.estimatedArrivalMinutes();

        if (
            arrivalMinutes == null
                || arrivalMinutes < MIN_ARRIVAL_MINUTES
                || arrivalMinutes > MAX_ARRIVAL_MINUTES
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El tiempo estimado de llegada debe estar entre "
                    + MIN_ARRIVAL_MINUTES + " y "
                    + MAX_ARRIVAL_MINUTES + " minutos."
            );
        }

        Integer validityMinutes = request.validityMinutes();

        if (
            validityMinutes != null
                && (
                    validityMinutes < MIN_VALIDITY_MINUTES
                    || validityMinutes > MAX_VALIDITY_MINUTES
                )
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La vigencia debe estar entre "
                    + MIN_VALIDITY_MINUTES + " y "
                    + MAX_VALIDITY_MINUTES + " minutos."
            );
        }
    }

    private void validateRequestCanReceiveProposal(
        MedicalRequest medicalRequest
    ) {
        if (!"PENDING".equals(medicalRequest.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La solicitud ya no admite propuestas. Estado actual: "
                    + medicalRequest.getStatus() + "."
            );
        }

        if (medicalRequest.getAcceptedProposal() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La solicitud ya tiene una propuesta aceptada."
            );
        }

        if (medicalRequest.getAcceptedSpecialistProfile() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La solicitud ya tiene un especialista asignado."
            );
        }
    }

    private void validateSpecialistCanPropose(
        MedicalRequest medicalRequest,
        SpecialistProfile specialist
    ) {
        if (!"ACTIVE".equals(specialist.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El especialista no se encuentra activo. Estado actual: "
                    + specialist.getStatus() + "."
            );
        }

        if (!specialist.isAvailable()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El especialista no se encuentra disponible."
            );
        }

        String requestProfessionCode =
            medicalRequest
                .getMedicalService()
                .getProfession()
                .getCode();

        String specialistProfessionCode =
            specialist
                .getProfession()
                .getCode();

        if (
            !Objects.equals(
                requestProfessionCode,
                specialistProfessionCode
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La profesion del especialista no corresponde al "
                    + "servicio solicitado."
            );
        }
    }

    private void validateCommercialProfile(
        SpecialistCommercialProfile commercialProfile
    ) {
        if (!commercialProfile.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El perfil comercial del especialista esta inactivo."
            );
        }
    }

    private int resolveValidityMinutes(
        Integer validityMinutes
    ) {
        return validityMinutes == null
            ? DEFAULT_VALIDITY_MINUTES
            : validityMinutes;
    }

    private String normalizeMobilityPolicy(
        String mobilityPolicy
    ) {
        if (
            mobilityPolicy == null
                || mobilityPolicy.isBlank()
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La politica de movilidad no esta configurada."
            );
        }

        return mobilityPolicy
            .trim()
            .toUpperCase(Locale.ROOT);
    }

    private BigDecimal resolveMobilityAmount(
        SpecialistCommercialProfile commercialProfile,
        String mobilityPolicy
    ) {
        return switch (mobilityPolicy) {
            case "INCLUDED" ->
                BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
                );

            case "SEPARATE" ->
                requirePositiveMoney(
                    commercialProfile
                        .getMobilityReferenceAmount(),
                    "La movilidad separada no tiene un importe valido."
                );

            case "NOT_AVAILABLE" ->
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El especialista no ofrece movilidad para "
                        + "atenciones domiciliarias."
                );

            default ->
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Politica de movilidad no reconocida: "
                        + mobilityPolicy
                );
        };
    }

    private BigDecimal requirePositiveMoney(
        BigDecimal amount,
        String errorMessage
    ) {
        if (
            amount == null
                || amount.signum() <= 0
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                errorMessage
            );
        }

        return amount.setScale(
            2,
            RoundingMode.HALF_UP
        );
    }

    private String cleanAndValidateMessage(
        String message
    ) {
        if (
            message == null
                || message.isBlank()
        ) {
            return null;
        }

        String cleanedMessage = message.trim();

        if (cleanedMessage.length() > 500) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El mensaje de la propuesta no puede superar "
                    + "500 caracteres."
            );
        }

        return cleanedMessage;
    }

    private MedicalRequestProposalResponse toResponse(
        MedicalRequestProposal proposal
    ) {
        MedicalRequest medicalRequest =
            proposal.getMedicalRequest();

        MedicalService medicalService =
            medicalRequest.getMedicalService();

        Profession profession =
            medicalService.getProfession();

        SpecialistProfile specialist =
            proposal.getSpecialistProfile();

        UserAccount specialistUser =
            specialist.getUserAccount();

        return new MedicalRequestProposalResponse(
            proposal.getId(),
            medicalRequest.getId(),
            medicalRequest.getRequestCode(),
            medicalRequest.getStatus(),
            specialist.getId(),
            buildFullName(specialistUser),
            profession.getCode(),
            profession.getName(),
            medicalService.getCode(),
            medicalService.getName(),
            proposal.getServiceAmount(),
            proposal.getMobilityPolicySnapshot(),
            proposal.getMobilityAmount(),
            proposal.getTotalAmount(),
            proposal.getCurrency(),
            proposal.getProposalMessage(),
            proposal.getEstimatedArrivalMinutes(),
            proposal.getStatus(),
            proposal.getExpiresAt(),
            proposal.getCreatedAt(),
            proposal.getRespondedAt(),
            proposal.getWithdrawnAt()
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
