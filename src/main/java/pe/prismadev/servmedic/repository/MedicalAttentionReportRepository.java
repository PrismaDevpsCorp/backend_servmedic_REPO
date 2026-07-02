package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.MedicalAttentionReport;

import java.util.Optional;

public interface MedicalAttentionReportRepository extends JpaRepository<MedicalAttentionReport, Long> {

    @Query("""
        select ar
        from MedicalAttentionReport ar
        join fetch ar.medicalRequest mr
        join fetch mr.patientProfile pp
        join fetch pp.userAccount pu
        join fetch mr.medicalService ms
        join fetch ms.profession p
        join fetch ar.specialistProfile sp
        join fetch sp.userAccount su
        where mr.id = :medicalRequestId
    """)
    Optional<MedicalAttentionReport> findDetailedByMedicalRequestId(@Param("medicalRequestId") Long medicalRequestId);
}