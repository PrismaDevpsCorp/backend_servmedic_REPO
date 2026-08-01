package pe.prismadev.servmedic.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.MedicalRequestProposal;

import java.util.List;
import java.util.Optional;

public interface MedicalRequestProposalRepository
    extends JpaRepository<MedicalRequestProposal, Long> {

    boolean existsByMedicalRequestIdAndSpecialistProfileIdAndStatus(
        Long medicalRequestId,
        Long specialistProfileId,
        String status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select proposal
        from MedicalRequestProposal proposal
        where proposal.medicalRequest.id = :medicalRequestId
          and proposal.specialistProfile.id = :specialistProfileId
          and proposal.status = 'PENDING'
        """)
    Optional<MedicalRequestProposal>
        findPendingByRequestAndSpecialistForUpdate(
            @Param("medicalRequestId") Long medicalRequestId,
            @Param("specialistProfileId") Long specialistProfileId
        );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select proposal
        from MedicalRequestProposal proposal
        join fetch proposal.medicalRequest medicalRequest
        join fetch medicalRequest.patientProfile patientProfile
        join fetch patientProfile.userAccount patientUser
        join fetch medicalRequest.medicalService medicalService
        join fetch medicalService.profession serviceProfession
        join fetch proposal.specialistProfile specialistProfile
        join fetch specialistProfile.userAccount specialistUser
        join fetch specialistProfile.profession specialistProfession
        where proposal.id = :proposalId
          and medicalRequest.id = :medicalRequestId
        """)
    Optional<MedicalRequestProposal>
        findDetailedByIdAndRequestIdForUpdate(
            @Param("proposalId") Long proposalId,
            @Param("medicalRequestId") Long medicalRequestId
        );

    @Query("""
        select proposal
        from MedicalRequestProposal proposal
        join fetch proposal.medicalRequest medicalRequest
        join fetch medicalRequest.patientProfile patientProfile
        join fetch patientProfile.userAccount patientUser
        join fetch medicalRequest.medicalService medicalService
        join fetch medicalService.profession serviceProfession
        join fetch proposal.specialistProfile specialistProfile
        join fetch specialistProfile.userAccount specialistUser
        join fetch specialistProfile.profession specialistProfession
        where medicalRequest.id = :medicalRequestId
        order by proposal.createdAt desc, proposal.id desc
        """)
    List<MedicalRequestProposal> findDetailedByRequestId(
        @Param("medicalRequestId") Long medicalRequestId
    );

    @Query("""
        select proposal
        from MedicalRequestProposal proposal
        join fetch proposal.medicalRequest medicalRequest
        join fetch medicalRequest.patientProfile patientProfile
        join fetch patientProfile.userAccount patientUser
        join fetch medicalRequest.medicalService medicalService
        join fetch medicalService.profession serviceProfession
        join fetch proposal.specialistProfile specialistProfile
        join fetch specialistProfile.userAccount specialistUser
        join fetch specialistProfile.profession specialistProfession
        where medicalRequest.id = :medicalRequestId
          and specialistProfile.id = :specialistProfileId
        order by proposal.createdAt desc, proposal.id desc
        """)
    List<MedicalRequestProposal>
        findDetailedByRequestIdAndSpecialistId(
            @Param("medicalRequestId") Long medicalRequestId,
            @Param("specialistProfileId") Long specialistProfileId
        );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select proposal
        from MedicalRequestProposal proposal
        where proposal.medicalRequest.id = :medicalRequestId
          and proposal.status = 'PENDING'
        order by proposal.id asc
        """)
    List<MedicalRequestProposal> findPendingByRequestIdForUpdate(
        @Param("medicalRequestId") Long medicalRequestId
    );
}
