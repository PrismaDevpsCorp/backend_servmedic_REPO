package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.SpecialistCommercialProfile;

import java.util.Optional;

public interface SpecialistCommercialProfileRepository
    extends JpaRepository<SpecialistCommercialProfile, Long> {

    @Query("""
        select scp
        from SpecialistCommercialProfile scp
        join fetch scp.specialistProfile sp
        join fetch sp.profession p
        where sp.id = :specialistProfileId
    """)
    Optional<SpecialistCommercialProfile> findDetailedBySpecialistProfileId(
        @Param("specialistProfileId") Long specialistProfileId
    );

    boolean existsBySpecialistProfileId(Long specialistProfileId);
}