package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.MedicalRequestResponse;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.MedicalService;
import pe.prismadev.servmedic.entity.PatientProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;
import pe.prismadev.servmedic.repository.PatientProfileRepository;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

import java.util.List;

@Service
public class MedicalRequestHistoryService {

    private final MedicalRequestRepository medicalRequestRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final SpecialistProfileRepository specialistProfileRepository;

    public MedicalRequestHistoryService(
        MedicalRequestRepository medicalRequestRepository,
        PatientProfileRepository patientProfileRepository,
        SpecialistProfileRepository specialistProfileRepository
    ) {
        this.medicalRequestRepository = medicalRequestRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.specialistProfileRepository = specialistProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<MedicalRequestResponse> listPatientRequests(Long patientProfileId, String status) {
        validatePatientExists(patientProfileId);
        String normalizedStatus = normalizeStatus(status);

        return medicalRequestRepository.findDetailedByPatientProfileId(patientProfileId, normalizedStatus)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRequestResponse> listSpecialistAssignedRequests(Long specialistProfileId, String status) {
        validateSpecialistExists(specialistProfileId);
        String normalizedStatus = normalizeStatus(status);

        return medicalRequestRepository.findDetailedByAcceptedSpecialistProfileId(specialistProfileId, normalizedStatus)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private void validatePatientExists(Long patientProfileId) {
        if (!patientProfileRepository.existsById(patientProfileId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de paciente no encontrado: " + patientProfileId
            );
        }
    }

    private void validateSpecialistExists(Long specialistProfileId) {
        if (!specialistProfileRepository.existsById(specialistProfileId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de especialista no encontrado: " + specialistProfileId
            );
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalized = status.trim().toUpperCase();

        return switch (normalized) {
            case "PENDING",
                 "ACCEPTED",
                 "EN_CAMINO",
                 "EN_ATENCION",
                 "FINALIZADO",
                 "CANCELADO_PACIENTE",
                 "CANCELADO_ESPECIALISTA",
                 "EXPIRED" -> normalized;
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Estado de solicitud no permitido: " + status
            );
        };
    }

    private MedicalRequestResponse toResponse(MedicalRequest request) {
        PatientProfile patientProfile = request.getPatientProfile();
        UserAccount patientUser = patientProfile.getUserAccount();
        MedicalService service = request.getMedicalService();

        Long acceptedSpecialistProfileId = null;
        String acceptedSpecialistFullName = null;

        if (request.getAcceptedSpecialistProfile() != null) {
            acceptedSpecialistProfileId = request.getAcceptedSpecialistProfile().getId();
            UserAccount specialistUser = request.getAcceptedSpecialistProfile().getUserAccount();
            acceptedSpecialistFullName = specialistUser.getFirstName() + " " + specialistUser.getLastName();
        }

        return new MedicalRequestResponse(
            request.getId(),
            request.getRequestCode(),
            patientProfile.getId(),
            patientUser.getFirstName() + " " + patientUser.getLastName(),
            service.getCode(),
            service.getName(),
            service.getProfession().getCode(),
            service.getProfession().getName(),
            service.isRequiresPrescription(),
            request.getStatus(),
            acceptedSpecialistProfileId,
            acceptedSpecialistFullName,
            request.getAddressText(),
            request.getAddressReference(),
            request.getLatitude(),
            request.getLongitude(),
            request.getPrescriptionImageUrl(),
            request.getPatientNotes(),
            request.getEstimatedAmount(),
            request.getDistanceKm(),
            request.getCreatedAt(),
            request.getAcceptedAt(),
            request.getStartedRouteAt(),
            request.getStartedAttentionAt(),
            request.getFinishedAt()
        );
    }
}