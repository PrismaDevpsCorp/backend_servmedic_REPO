package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.SpecialistWalletTransaction;

import java.math.BigDecimal;

public interface SpecialistWalletTransactionRepository extends JpaRepository<SpecialistWalletTransaction, Long> {

    @Query("""
        select coalesce(sum(wt.amount), 0)
        from SpecialistWalletTransaction wt
        where wt.specialistProfile.id = :specialistProfileId
          and wt.status = 'AVAILABLE'
    """)
    BigDecimal getAvailableBalance(@Param("specialistProfileId") Long specialistProfileId);
}