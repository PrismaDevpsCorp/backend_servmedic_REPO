package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.prismadev.servmedic.entity.SpecialistOfferedService;

public interface SpecialistOfferedServiceRepository extends JpaRepository<SpecialistOfferedService, Long> {

    boolean existsBySpecialistProfileIdAndMedicalServiceIdAndActiveTrue(
        Long specialistProfileId,
        Long medicalServiceId
    );
}