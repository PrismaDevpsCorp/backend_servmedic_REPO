package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.prismadev.servmedic.entity.Profession;

import java.util.List;
import java.util.Optional;

public interface ProfessionRepository extends JpaRepository<Profession, Long> {

    Optional<Profession> findByCode(String code);

    List<Profession> findByActiveTrueOrderByIdAsc();
}