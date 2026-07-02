package pe.prismadev.servmedic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.prismadev.servmedic.entity.PatientProfile;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
}