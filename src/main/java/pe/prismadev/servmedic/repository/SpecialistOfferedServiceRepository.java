package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.SpecialistOfferedService;

import java.util.List;

public interface SpecialistOfferedServiceRepository
    extends JpaRepository<SpecialistOfferedService, Long> {

    boolean existsBySpecialistProfileIdAndMedicalServiceIdAndActiveTrue(
        Long specialistProfileId,
        Long medicalServiceId
    );

    @Query("""
        select sos
        from SpecialistOfferedService sos
        join fetch sos.medicalService ms
        join fetch ms.profession p
        where sos.specialistProfile.id = :specialistProfileId
        order by ms.displayOrder asc, ms.id asc
    """)
    List<SpecialistOfferedService> findDetailedBySpecialistProfileId(
        @Param("specialistProfileId") Long specialistProfileId
    );
}