package pe.prismadev.servmedic.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.MedicalRequest;

import java.util.List;
import java.util.Optional;

public interface MedicalRequestRepository extends JpaRepository<MedicalRequest, Long> {

    @Query("""
        select mr
        from MedicalRequest mr
        join fetch mr.patientProfile pp
        join fetch pp.userAccount u
        join fetch mr.medicalService ms
        join fetch ms.profession p
        left join fetch mr.acceptedSpecialistProfile asp
        left join fetch asp.userAccount su
        where mr.id = :id
    """)
    Optional<MedicalRequest> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select mr
        from MedicalRequest mr
        join fetch mr.patientProfile pp
        join fetch pp.userAccount u
        join fetch mr.medicalService ms
        join fetch ms.profession p
        left join fetch mr.acceptedSpecialistProfile asp
        where mr.id = :id
    """)
    Optional<MedicalRequest> findDetailedByIdForUpdate(@Param("id") Long id);

    @Query("""
        select mr
        from MedicalRequest mr
        join fetch mr.patientProfile pp
        join fetch pp.userAccount u
        join fetch mr.medicalService ms
        join fetch ms.profession p
        where mr.status = 'PENDING'
          and p.code = :professionCode
        order by mr.createdAt asc
    """)
    List<MedicalRequest> findPendingByProfessionCode(@Param("professionCode") String professionCode);

    @Query("""
        select mr
        from MedicalRequest mr
        join fetch mr.patientProfile pp
        join fetch pp.userAccount u
        join fetch mr.medicalService ms
        join fetch ms.profession p
        left join fetch mr.acceptedSpecialistProfile asp
        left join fetch asp.userAccount su
        where pp.id = :patientProfileId
          and (:status is null or mr.status = :status)
        order by mr.createdAt desc, mr.id desc
    """)
    List<MedicalRequest> findDetailedByPatientProfileId(
        @Param("patientProfileId") Long patientProfileId,
        @Param("status") String status
    );

    @Query("""
        select mr
        from MedicalRequest mr
        join fetch mr.patientProfile pp
        join fetch pp.userAccount u
        join fetch mr.medicalService ms
        join fetch ms.profession p
        join fetch mr.acceptedSpecialistProfile asp
        join fetch asp.userAccount su
        where asp.id = :specialistProfileId
          and (:status is null or mr.status = :status)
        order by mr.acceptedAt desc, mr.createdAt desc, mr.id desc
    """)
    List<MedicalRequest> findDetailedByAcceptedSpecialistProfileId(
        @Param("specialistProfileId") Long specialistProfileId,
        @Param("status") String status
    );
}