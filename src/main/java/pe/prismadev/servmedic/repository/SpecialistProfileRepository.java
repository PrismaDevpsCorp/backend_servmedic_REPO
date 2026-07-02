package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.SpecialistProfile;

import java.util.List;
import java.util.Optional;

public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long> {

    @Query("""
        select sp
        from SpecialistProfile sp
        join fetch sp.userAccount u
        join fetch sp.profession p
        where sp.id = :id
    """)
    Optional<SpecialistProfile> findDetailedById(@Param("id") Long id);

    @Query("""
        select sp
        from SpecialistProfile sp
        join fetch sp.userAccount u
        join fetch sp.profession p
        where (:status is null or sp.status = :status)
        order by sp.id desc
    """)
    List<SpecialistProfile> findDetailedAll(@Param("status") String status);
}