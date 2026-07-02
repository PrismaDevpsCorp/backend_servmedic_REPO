package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.AdminSpecialistResponse;
import pe.prismadev.servmedic.dto.UpdateSpecialistStatusRequest;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

import java.util.List;

@Service
public class AdminSpecialistService {

    private final SpecialistProfileRepository specialistProfileRepository;

    public AdminSpecialistService(SpecialistProfileRepository specialistProfileRepository) {
        this.specialistProfileRepository = specialistProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminSpecialistResponse> listSpecialists(String status) {
        String normalizedStatus = normalizeStatusNullable(status);

        return specialistProfileRepository.findDetailedAll(normalizedStatus)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public AdminSpecialistResponse updateStatus(Long specialistProfileId, UpdateSpecialistStatusRequest request) {
        String normalizedStatus = normalizeStatusRequired(request.status());

        SpecialistProfile specialist = specialistProfileRepository.findDetailedById(specialistProfileId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de especialista no encontrado: " + specialistProfileId
            ));

        specialist.updateStatusFromAdmin(normalizedStatus);

        SpecialistProfile saved = specialistProfileRepository.save(specialist);

        return toResponse(saved);
    }

    private String normalizeStatusNullable(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return normalizeStatusRequired(status);
    }

    private String normalizeStatusRequired(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();

        return switch (normalized) {
            case "PENDING_VALIDATION",
                 "PENDING_INTERVIEW",
                 "REJECTED_INHABILITATION",
                 "REJECTED_EVALUATION",
                 "ACTIVE",
                 "SUSPENDED" -> normalized;
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Estado de especialista no permitido: " + status
            );
        };
    }

    private AdminSpecialistResponse toResponse(SpecialistProfile specialist) {
        UserAccount user = specialist.getUserAccount();

        return new AdminSpecialistResponse(
            specialist.getId(),
            user.getId(),
            user.getFirstName() + " " + user.getLastName(),
            user.getEmail(),
            user.getDni(),
            user.getMobilePhone(),
            specialist.getProfession().getCode(),
            specialist.getProfession().getName(),
            specialist.getCollegeNumber(),
            specialist.getStatus(),
            specialist.isAvailable(),
            specialist.getRatingAverage(),
            specialist.getRatingCount()
        );
    }
}