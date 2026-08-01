package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.SpecialistOfferedService;

import java.util.List;
import java.util.Optional;

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

    @Query("""
        select sos
        from SpecialistOfferedService sos
        join fetch sos.medicalService ms
        join fetch ms.profession p
        where sos.specialistProfile.id = :specialistProfileId
          and ms.id = :medicalServiceId
          and sos.active = true
        """)
    Optional<SpecialistOfferedService>
        findActiveDetailedBySpecialistProfileIdAndMedicalServiceId(
            @Param("specialistProfileId") Long specialistProfileId,
            @Param("medicalServiceId") Long medicalServiceId
        );
}
