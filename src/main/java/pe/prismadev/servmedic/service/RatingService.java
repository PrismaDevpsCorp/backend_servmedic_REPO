package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CreatePatientRatingRequest;
import pe.prismadev.servmedic.dto.CreateSpecialistRatingRequest;
import pe.prismadev.servmedic.dto.MedicalRequestRatingsResponse;
import pe.prismadev.servmedic.dto.RatingResponse;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.ServiceRating;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;
import pe.prismadev.servmedic.repository.ServiceRatingRepository;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

import java.util.List;

@Service
public class RatingService {

    private static final String RATER_PATIENT = "PATIENT";
    private static final String RATER_SPECIALIST = "SPECIALIST";

    private final MedicalRequestRepository medicalRequestRepository;
    private final ServiceRatingRepository serviceRatingRepository;
    private final SpecialistProfileRepository specialistProfileRepository;

    public RatingService(
        MedicalRequestRepository medicalRequestRepository,
        ServiceRatingRepository serviceRatingRepository,
        SpecialistProfileRepository specialistProfileRepository
    ) {
        this.medicalRequestRepository = medicalRequestRepository;
        this.serviceRatingRepository = serviceRatingRepository;
        this.specialistProfileRepository = specialistProfileRepository;
    }

    @Transactional
    public RatingResponse rateSpecialist(Long medicalRequestId, CreatePatientRatingRequest request) {
        MedicalRequest medicalRequest = findRequest(medicalRequestId);
        validateFinished(medicalRequest);

        if (!medicalRequest.getPatientProfile().getId().equals(request.patientProfileId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo el paciente dueño de la solicitud puede calificar al especialista."
            );
        }

        validateAssignedSpecialist(medicalRequest);

        if (serviceRatingRepository.existsByMedicalRequestIdAndRaterRole(medicalRequestId, RATER_PATIENT)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El paciente ya califico esta solicitud."
            );
        }

        ServiceRating rating = new ServiceRating();
        rating.setMedicalRequest(medicalRequest);
        rating.setRaterRole(RATER_PATIENT);
        rating.setPatientProfile(medicalRequest.getPatientProfile());
        rating.setSpecialistProfile(medicalRequest.getAcceptedSpecialistProfile());
        rating.setScore(request.score());
        rating.setComment(cleanNullable(request.comment()));

        serviceRatingRepository.save(rating);

        SpecialistProfile specialist = medicalRequest.getAcceptedSpecialistProfile();
        specialist.applyNewPatientRating(request.score());
        specialistProfileRepository.save(specialist);

        ServiceRating detailed = serviceRatingRepository
            .findDetailedByMedicalRequestIdAndRaterRole(medicalRequestId, RATER_PATIENT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo recuperar la calificacion registrada."
            ));

        return toResponse(detailed, "Calificacion del paciente registrada correctamente.");
    }

    @Transactional
    public RatingResponse ratePatient(Long medicalRequestId, CreateSpecialistRatingRequest request) {
        MedicalRequest medicalRequest = findRequest(medicalRequestId);
        validateFinished(medicalRequest);
        validateAssignedSpecialist(medicalRequest);

        if (!medicalRequest.getAcceptedSpecialistProfile().getId().equals(request.specialistProfileId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo el especialista asignado puede calificar al paciente."
            );
        }

        if (serviceRatingRepository.existsByMedicalRequestIdAndRaterRole(medicalRequestId, RATER_SPECIALIST)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El especialista ya califico esta solicitud."
            );
        }

        ServiceRating rating = new ServiceRating();
        rating.setMedicalRequest(medicalRequest);
        rating.setRaterRole(RATER_SPECIALIST);
        rating.setPatientProfile(medicalRequest.getPatientProfile());
        rating.setSpecialistProfile(medicalRequest.getAcceptedSpecialistProfile());
        rating.setScore(request.score());
        rating.setComment(cleanNullable(request.comment()));

        serviceRatingRepository.save(rating);

        ServiceRating detailed = serviceRatingRepository
            .findDetailedByMedicalRequestIdAndRaterRole(medicalRequestId, RATER_SPECIALIST)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo recuperar la calificacion registrada."
            ));

        return toResponse(detailed, "Calificacion del especialista registrada correctamente.");
    }

    @Transactional(readOnly = true)
    public MedicalRequestRatingsResponse listRatings(Long medicalRequestId) {
        MedicalRequest medicalRequest = findRequest(medicalRequestId);

        List<RatingResponse> ratings = serviceRatingRepository.findDetailedByMedicalRequestId(medicalRequestId)
            .stream()
            .map(rating -> toResponse(rating, "Calificacion encontrada."))
            .toList();

        return new MedicalRequestRatingsResponse(
            medicalRequest.getId(),
            medicalRequest.getRequestCode(),
            medicalRequest.getStatus(),
            ratings
        );
    }

    private MedicalRequest findRequest(Long medicalRequestId) {
        return medicalRequestRepository.findDetailedById(medicalRequestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + medicalRequestId
            ));
    }

    private void validateFinished(MedicalRequest medicalRequest) {
        if (!"FINALIZADO".equals(medicalRequest.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se puede calificar una solicitud FINALIZADA. Estado actual: " + medicalRequest.getStatus()
            );
        }
    }

    private void validateAssignedSpecialist(MedicalRequest medicalRequest) {
        if (medicalRequest.getAcceptedSpecialistProfile() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud no tiene especialista asignado."
            );
        }
    }

    private RatingResponse toResponse(ServiceRating rating, String message) {
        MedicalRequest request = rating.getMedicalRequest();
        UserAccount patientUser = rating.getPatientProfile().getUserAccount();
        UserAccount specialistUser = rating.getSpecialistProfile().getUserAccount();

        return new RatingResponse(
            rating.getId(),
            request.getId(),
            request.getRequestCode(),
            rating.getRaterRole(),
            rating.getScore(),
            rating.getComment(),
            rating.getPatientProfile().getId(),
            patientUser.getFirstName() + " " + patientUser.getLastName(),
            rating.getSpecialistProfile().getId(),
            specialistUser.getFirstName() + " " + specialistUser.getLastName(),
            rating.getSpecialistProfile().getRatingAverage(),
            rating.getSpecialistProfile().getRatingCount(),
            rating.getCreatedAt(),
            message
        );
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}