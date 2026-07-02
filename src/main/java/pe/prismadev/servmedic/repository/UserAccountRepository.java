package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.prismadev.servmedic.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);
}