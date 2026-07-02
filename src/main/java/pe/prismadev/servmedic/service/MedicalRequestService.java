package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CreateMedicalRequestRequest;
import pe.prismadev.servmedic.dto.CreatePatientMedicalRequestRequest;
import pe.prismadev.servmedic.dto.MedicalRequestResponse;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.MedicalService;
import pe.prismadev.servmedic.entity.PatientProfile;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;
import pe.prismadev.servmedic.repository.MedicalServiceRepository;
import pe.prismadev.servmedic.repository.PatientProfileRepository;
import pe.prismadev.servmedic.repository.SpecialistOfferedServiceRepository;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class MedicalRequestService {

    private final MedicalRequestRepository medicalRequestRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final SpecialistProfileRepository specialistProfileRepository;
    private final SpecialistOfferedServiceRepository specialistOfferedServiceRepository;

    public MedicalRequestService(
        MedicalRequestRepository medicalRequestRepository,
        PatientProfileRepository patientProfileRepository,
        MedicalServiceRepository medicalServiceRepository,
        SpecialistProfileRepository specialistProfileRepository,
        SpecialistOfferedServiceRepository specialistOfferedServiceRepository
    ) {
        this.medicalRequestRepository = medicalRequestRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.medicalServiceRepository = medicalServiceRepository;
        this.specialistProfileRepository = specialistProfileRepository;
        this.specialistOfferedServiceRepository = specialistOfferedServiceRepository;
    }

    @Transactional
    public MedicalRequestResponse create(CreateMedicalRequestRequest request) {
        return createInternal(
            request.patientProfileId(),
            request.serviceCode(),
            request.addressText(),
            request.addressReference(),
            request.latitude(),
            request.longitude(),
            request.prescriptionImageUrl(),
            request.patientNotes()
        );
    }

    @Transactional
    public MedicalRequestResponse createForAuthenticatedPatient(
        Long patientProfileId,
        CreatePatientMedicalRequestRequest request
    ) {
        if (patientProfileId == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se pudo identificar el perfil de paciente autenticado."
            );
        }

        return createInternal(
            patientProfileId,
            request.serviceCode(),
            request.addressText(),
            request.addressReference(),
            request.latitude(),
            request.longitude(),
            request.prescriptionImageUrl(),
            request.patientNotes()
        );
    }

    private MedicalRequestResponse createInternal(
        Long patientProfileId,
        String serviceCode,
        String addressText,
        String addressReference,
        BigDecimal latitude,
        BigDecimal longitude,
        String prescriptionImageUrl,
        String patientNotes
    ) {
        PatientProfile patientProfile = patientProfileRepository.findById(patientProfileId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de paciente no encontrado: " + patientProfileId
            ));

        MedicalService medicalService = medicalServiceRepository.findActiveByCode(serviceCode)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Servicio medico no encontrado o inactivo: " + serviceCode
            ));

        if (medicalService.isRequiresPrescription() && isBlank(prescriptionImageUrl)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El servicio seleccionado requiere receta previa. Debe adjuntar una imagen o URL de receta."
            );
        }

        MedicalRequest medicalRequest = new MedicalRequest();
        medicalRequest.setRequestCode(generateRequestCode());
        medicalRequest.setPatientProfile(patientProfile);
        medicalRequest.setMedicalService(medicalService);
        medicalRequest.setAddressText(addressText.trim());
        medicalRequest.setAddressReference(cleanNullable(addressReference));
        medicalRequest.setLatitude(latitude);
        medicalRequest.setLongitude(longitude);
        medicalRequest.setPrescriptionImageUrl(cleanNullable(prescriptionImageUrl));
        medicalRequest.setPatientNotes(cleanNullable(patientNotes));
        medicalRequest.setEstimatedAmount(calculateEstimatedAmount(medicalService));
        medicalRequest.setDistanceKm(null);

        MedicalRequest saved = medicalRequestRepository.save(medicalRequest);

        MedicalRequest detailed = medicalRequestRepository.findDetailedById(saved.getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo recuperar la solicitud creada."
            ));

        return toResponse(detailed);
    }

    @Transactional
    public MedicalRequestResponse acceptRequest(Long requestId, Long specialistProfileId) {
        MedicalRequest request = medicalRequestRepository.findDetailedByIdForUpdate(requestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + requestId
            ));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud no esta disponible para aceptar. Estado actual: " + request.getStatus()
            );
        }

        SpecialistProfile specialist = specialistProfileRepository.findDetailedById(specialistProfileId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de especialista no encontrado: " + specialistProfileId
            ));

        validateSpecialistCanAccept(request, specialist);

        request.acceptBy(specialist);
        MedicalRequest saved = medicalRequestRepository.save(request);

        return reloadAndMap(saved.getId(), "No se pudo recuperar la solicitud aceptada.");
    }

    @Transactional
    public MedicalRequestResponse startRoute(Long requestId, Long specialistProfileId) {
        MedicalRequest request = medicalRequestRepository.findDetailedByIdForUpdate(requestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + requestId
            ));

        validateAssignedSpecialist(request, specialistProfileId);
        validateCurrentStatus(request, "ACCEPTED", "iniciar ruta");

        request.startRoute();
        MedicalRequest saved = medicalRequestRepository.save(request);

        return reloadAndMap(saved.getId(), "No se pudo recuperar la solicitud en camino.");
    }

    @Transactional
    public MedicalRequestResponse startAttention(Long requestId, Long specialistProfileId) {
        MedicalRequest request = medicalRequestRepository.findDetailedByIdForUpdate(requestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + requestId
            ));

        validateAssignedSpecialist(request, specialistProfileId);
        validateCurrentStatus(request, "EN_CAMINO", "iniciar atencion");

        request.startAttention();
        MedicalRequest saved = medicalRequestRepository.save(request);

        return reloadAndMap(saved.getId(), "No se pudo recuperar la solicitud en atencion.");
    }

    @Transactional
    public MedicalRequestResponse finish(Long requestId, Long specialistProfileId) {
        MedicalRequest request = medicalRequestRepository.findDetailedByIdForUpdate(requestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + requestId
            ));

        validateAssignedSpecialist(request, specialistProfileId);
        validateCurrentStatus(request, "EN_ATENCION", "finalizar servicio");

        request.finish();
        MedicalRequest saved = medicalRequestRepository.save(request);

        return reloadAndMap(saved.getId(), "No se pudo recuperar la solicitud finalizada.");
    }

    @Transactional(readOnly = true)
    public MedicalRequestResponse findById(Long id) {
        MedicalRequest request = medicalRequestRepository.findDetailedById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + id
            ));

        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public MedicalRequestResponse findPatientRequestById(Long patientProfileId, Long id) {
        MedicalRequest request = medicalRequestRepository.findDetailedById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + id
            ));

        Long ownerPatientProfileId = request.getPatientProfile().getId();

        if (!ownerPatientProfileId.equals(patientProfileId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tiene permiso para consultar esta solicitud medica."
            );
        }

        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public List<MedicalRequestResponse> listPendingByProfession(String professionCode) {
        return medicalRequestRepository.findPendingByProfessionCode(professionCode)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRequestResponse> listPendingForAuthenticatedSpecialist(Long specialistProfileId) {
        if (specialistProfileId == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se pudo identificar el perfil de especialista autenticado."
            );
        }

        SpecialistProfile specialist = specialistProfileRepository.findDetailedById(specialistProfileId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de especialista no encontrado: " + specialistProfileId
            ));

        if (!"ACTIVE".equals(specialist.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El especialista no esta activo. Estado actual: " + specialist.getStatus()
            );
        }

        String professionCode = specialist.getProfession().getCode();

        return medicalRequestRepository.findPendingByProfessionCode(professionCode)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private void validateSpecialistCanAccept(MedicalRequest request, SpecialistProfile specialist) {
        if (!"ACTIVE".equals(specialist.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El especialista no esta activo. Estado actual: " + specialist.getStatus()
            );
        }

        if (!specialist.isAvailable()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El especialista no esta disponible."
            );
        }

        String requestProfession = request.getMedicalService().getProfession().getCode();
        String specialistProfession = specialist.getProfession().getCode();

        if (!requestProfession.equals(specialistProfession)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Regla antintrusismo: la solicitud requiere profesion "
                    + requestProfession + " pero el especialista pertenece a " + specialistProfession
            );
        }

        boolean offersService = specialistOfferedServiceRepository
            .existsBySpecialistProfileIdAndMedicalServiceIdAndActiveTrue(
                specialist.getId(),
                request.getMedicalService().getId()
            );

        if (!offersService) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El especialista no tiene configurado este servicio dentro de sus servicios ofrecidos."
            );
        }
    }

    private void validateAssignedSpecialist(MedicalRequest request, Long specialistProfileId) {
        if (request.getAcceptedSpecialistProfile() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud no tiene especialista asignado."
            );
        }

        Long assignedSpecialistId = request.getAcceptedSpecialistProfile().getId();

        if (!assignedSpecialistId.equals(specialistProfileId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo el especialista asignado puede cambiar el estado de esta solicitud."
            );
        }
    }

    private void validateCurrentStatus(MedicalRequest request, String expectedStatus, String actionName) {
        if (!expectedStatus.equals(request.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se puede " + actionName + ". Estado requerido: "
                    + expectedStatus + ". Estado actual: " + request.getStatus()
            );
        }
    }

    private MedicalRequestResponse reloadAndMap(Long requestId, String errorMessage) {
        MedicalRequest detailed = medicalRequestRepository.findDetailedById(requestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                errorMessage
            ));

        return toResponse(detailed);
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

    private String generateRequestCode() {
        return "SM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal calculateEstimatedAmount(MedicalService medicalService) {
        String professionCode = medicalService.getProfession().getCode();

        return switch (professionCode) {
            case "MEDICO_GENERAL" -> new BigDecimal("90.00");
            case "ENFERMERIA" -> new BigDecimal("70.00");
            case "TERAPIA_FISICA_REHABILITACION" -> new BigDecimal("80.00");
            case "PSICOLOGIA" -> new BigDecimal("85.00");
            default -> new BigDecimal("75.00");
        };
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}