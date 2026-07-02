package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.prismadev.servmedic.entity.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    List<Role> findByActiveTrueOrderByIdAsc();
}