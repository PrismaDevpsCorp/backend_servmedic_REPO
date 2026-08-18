package pe.prismadev.servmedic.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.MedicalPayment;

import java.math.BigDecimal;
import java.util.Optional;

public interface MedicalPaymentRepository
    extends JpaRepository<MedicalPayment, Long> {

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
    Optional<MedicalPayment> findDetailedByMedicalRequestId(
        @Param("medicalRequestId") Long medicalRequestId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
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
    Optional<MedicalPayment> findDetailedByMedicalRequestIdForUpdate(
        @Param("medicalRequestId") Long medicalRequestId
    );

    @EntityGraph(attributePaths = {
        "medicalRequest",
        "medicalRequest.medicalService",
        "patientProfile",
        "patientProfile.userAccount",
        "specialistProfile"
    })
    Page<MedicalPayment>
    findBySpecialistProfile_IdAndPaymentFlow(
        Long specialistProfileId,
        String paymentFlow,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT
                COUNT(*) AS "totalOperations",

                COUNT(*) FILTER (
                    WHERE status = 'PAID'
                ) AS "paidOperations",

                COUNT(*) FILTER (
                    WHERE status = 'PENDING'
                ) AS "pendingOperations",

                COUNT(*) FILTER (
                    WHERE status = 'REJECTED'
                ) AS "rejectedOperations",

                COALESCE(
                    SUM(service_amount) FILTER (
                        WHERE status = 'PAID'
                    ),
                    0
                ) AS "paidServiceAmount",

                COALESCE(
                    SUM(mobility_amount) FILTER (
                        WHERE status = 'PAID'
                    ),
                    0
                ) AS "paidMobilityAmount",

                COALESCE(
                    SUM(additional_amount) FILTER (
                        WHERE status = 'PAID'
                    ),
                    0
                ) AS "paidAdditionalAmount",

                COALESCE(
                    SUM(amount) FILTER (
                        WHERE status = 'PAID'
                    ),
                    0
                ) AS "paidTotalAmount",

                COALESCE(
                    SUM(platform_commission_amount) FILTER (
                        WHERE status = 'PAID'
                    ),
                    0
                ) AS "platformCommissionAmount",

                COALESCE(
                    SUM(specialist_net_amount) FILTER (
                        WHERE status = 'PAID'
                    ),
                    0
                ) AS "specialistNetAmount"

            FROM medical_payments
            WHERE specialist_profile_id =
                :specialistProfileId
              AND payment_flow =
                'DIRECT_EXTERNAL'
            """,
        nativeQuery = true
    )
    SpecialistFinancialSummaryProjection
    summarizeDirectPaymentsForSpecialist(
        @Param("specialistProfileId")
        Long specialistProfileId
    );

    interface SpecialistFinancialSummaryProjection {

        Long getTotalOperations();

        Long getPaidOperations();

        Long getPendingOperations();

        Long getRejectedOperations();

        BigDecimal getPaidServiceAmount();

        BigDecimal getPaidMobilityAmount();

        BigDecimal getPaidAdditionalAmount();

        BigDecimal getPaidTotalAmount();

        BigDecimal getPlatformCommissionAmount();

        BigDecimal getSpecialistNetAmount();
    }
}
