package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.MedicalPayment;

import java.util.Optional;

public interface MedicalPaymentRepository extends JpaRepository<MedicalPayment, Long> {

    boolean existsByMedicalRequestId(Long medicalRequestId);

    @Query("""
        select mp
        from MedicalPayment mp
        join fetch mp.medicalRequest mr
        join fetch mp.patientProfile pp
        join fetch pp.userAccount pu
        join fetch mp.specialistProfile sp
        join fetch sp.userAccount su
        where mr.id = :medicalRequestId
    """)
    Optional<MedicalPayment> findDetailedByMedicalRequestId(@Param("medicalRequestId") Long medicalRequestId);
}