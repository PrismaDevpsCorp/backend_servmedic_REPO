package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.prismadev.servmedic.entity.SpecialistPaymentMethod;

import java.util.List;
import java.util.Optional;

public interface SpecialistPaymentMethodRepository
    extends JpaRepository<SpecialistPaymentMethod, Long> {

    @Query("""
        select spm
        from SpecialistPaymentMethod spm
        join fetch spm.paymentMethod pm
        where spm.specialistProfile.id = :specialistProfileId
        order by pm.id asc
    """)
    List<SpecialistPaymentMethod> findDetailedBySpecialistProfileId(
        @Param("specialistProfileId") Long specialistProfileId
    );

    Optional<SpecialistPaymentMethod>
        findBySpecialistProfileIdAndPaymentMethodId(
            Long specialistProfileId,
            Long paymentMethodId
        );
}