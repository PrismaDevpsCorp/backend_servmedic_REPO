package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.prismadev.servmedic.entity.PaymentMethod;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository
    extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findAllByActiveTrueOrderByIdAsc();

    Optional<PaymentMethod> findByCodeAndActiveTrue(String code);
}