package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.MedicalService;

import java.util.List;
import java.util.Optional;

public interface MedicalServiceRepository extends JpaRepository<MedicalService, Long> {

    @Query("""
        select ms
        from MedicalService ms
        join fetch ms.profession p
        where ms.active = true
        order by p.id asc, ms.displayOrder asc, ms.id asc
    """)
    List<MedicalService> findAllActiveWithProfession();

    @Query("""
        select ms
        from MedicalService ms
        join fetch ms.profession p
        where p.code = :professionCode
          and p.active = true
          and ms.active = true
        order by ms.displayOrder asc, ms.id asc
    """)
    List<MedicalService> findActiveByProfessionCode(@Param("professionCode") String professionCode);

    @Query("""
        select ms
        from MedicalService ms
        join fetch ms.profession p
        where ms.code = :serviceCode
          and ms.active = true
    """)
    Optional<MedicalService> findActiveByCode(@Param("serviceCode") String serviceCode);

    @Query("""
        select ms
        from MedicalService ms
        join fetch ms.profession p
        where ms.code in :serviceCodes
          and ms.active = true
    """)
    List<MedicalService> findActiveByCodes(@Param("serviceCodes") List<String> serviceCodes);
}