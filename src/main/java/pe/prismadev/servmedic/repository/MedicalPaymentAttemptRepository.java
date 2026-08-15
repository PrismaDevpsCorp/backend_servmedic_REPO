package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.MedicalPaymentAttempt;

public interface MedicalPaymentAttemptRepository
    extends JpaRepository<MedicalPaymentAttempt, Long> {

    @Query("""
        select count(attempt)
        from MedicalPaymentAttempt attempt
        where attempt.medicalPayment.id = :medicalPaymentId
        """)
    long countByMedicalPaymentId(
        @Param("medicalPaymentId") Long medicalPaymentId
    );
}
