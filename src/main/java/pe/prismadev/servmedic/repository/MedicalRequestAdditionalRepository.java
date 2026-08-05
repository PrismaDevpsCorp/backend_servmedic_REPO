package pe.prismadev.servmedic.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.MedicalRequestAdditional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MedicalRequestAdditionalRepository
    extends JpaRepository<MedicalRequestAdditional, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select additional
        from MedicalRequestAdditional additional
        join fetch additional.medicalRequest medicalRequest
        join fetch medicalRequest.patientProfile patientProfile
        join fetch patientProfile.userAccount patientUser
        join fetch medicalRequest.medicalService medicalService
        join fetch medicalRequest.acceptedSpecialistProfile acceptedSpecialist
        join fetch acceptedSpecialist.userAccount acceptedSpecialistUser
        join fetch medicalRequest.acceptedProposal acceptedProposal
        join fetch additional.specialistProfile specialistProfile
        join fetch specialistProfile.userAccount specialistUser
        where additional.id = :additionalId
          and medicalRequest.id = :medicalRequestId
        """)
    Optional<MedicalRequestAdditional>
        findDetailedByIdAndRequestIdForUpdate(
            @Param("additionalId") Long additionalId,
            @Param("medicalRequestId") Long medicalRequestId
        );

    @Query("""
        select additional
        from MedicalRequestAdditional additional
        join fetch additional.medicalRequest medicalRequest
        join fetch medicalRequest.patientProfile patientProfile
        join fetch patientProfile.userAccount patientUser
        join fetch medicalRequest.medicalService medicalService
        join fetch medicalRequest.acceptedSpecialistProfile acceptedSpecialist
        join fetch acceptedSpecialist.userAccount acceptedSpecialistUser
        join fetch medicalRequest.acceptedProposal acceptedProposal
        join fetch additional.specialistProfile specialistProfile
        join fetch specialistProfile.userAccount specialistUser
        where medicalRequest.id = :medicalRequestId
        order by additional.createdAt asc, additional.id asc
        """)
    List<MedicalRequestAdditional> findDetailedByRequestId(
        @Param("medicalRequestId") Long medicalRequestId
    );

    @Query("""
        select additional
        from MedicalRequestAdditional additional
        join fetch additional.medicalRequest medicalRequest
        join fetch medicalRequest.patientProfile patientProfile
        join fetch patientProfile.userAccount patientUser
        join fetch medicalRequest.medicalService medicalService
        join fetch medicalRequest.acceptedSpecialistProfile acceptedSpecialist
        join fetch acceptedSpecialist.userAccount acceptedSpecialistUser
        join fetch medicalRequest.acceptedProposal acceptedProposal
        join fetch additional.specialistProfile specialistProfile
        join fetch specialistProfile.userAccount specialistUser
        where medicalRequest.id = :medicalRequestId
          and specialistProfile.id = :specialistProfileId
        order by additional.createdAt asc, additional.id asc
        """)
    List<MedicalRequestAdditional>
        findDetailedByRequestIdAndSpecialistId(
            @Param("medicalRequestId") Long medicalRequestId,
            @Param("specialistProfileId") Long specialistProfileId
        );

    @Query("""
        select coalesce(sum(additional.amount), 0)
        from MedicalRequestAdditional additional
        where additional.medicalRequest.id = :medicalRequestId
          and additional.status = 'APPROVED'
        """)
    BigDecimal sumApprovedAmountByRequestId(
        @Param("medicalRequestId") Long medicalRequestId
    );

    boolean existsByMedicalRequestIdAndStatus(
        Long medicalRequestId,
        String status
    );
}
