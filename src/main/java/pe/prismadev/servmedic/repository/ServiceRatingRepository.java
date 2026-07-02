package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.ServiceRating;

import java.util.List;
import java.util.Optional;

public interface ServiceRatingRepository extends JpaRepository<ServiceRating, Long> {

    boolean existsByMedicalRequestIdAndRaterRole(Long medicalRequestId, String raterRole);

    @Query("""
        select sr
        from ServiceRating sr
        join fetch sr.medicalRequest mr
        join fetch sr.patientProfile pp
        join fetch pp.userAccount pu
        join fetch sr.specialistProfile sp
        join fetch sp.userAccount su
        where mr.id = :medicalRequestId
        order by sr.id asc
    """)
    List<ServiceRating> findDetailedByMedicalRequestId(@Param("medicalRequestId") Long medicalRequestId);

    @Query("""
        select sr
        from ServiceRating sr
        join fetch sr.medicalRequest mr
        join fetch sr.patientProfile pp
        join fetch pp.userAccount pu
        join fetch sr.specialistProfile sp
        join fetch sp.userAccount su
        where mr.id = :medicalRequestId
          and sr.raterRole = :raterRole
    """)
    Optional<ServiceRating> findDetailedByMedicalRequestIdAndRaterRole(
        @Param("medicalRequestId") Long medicalRequestId,
        @Param("raterRole") String raterRole
    );
}