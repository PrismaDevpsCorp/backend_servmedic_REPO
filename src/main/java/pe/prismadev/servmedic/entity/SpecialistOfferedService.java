package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "specialist_offered_services")
public class SpecialistOfferedService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_profile_id", nullable = false)
    private SpecialistProfile specialistProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_service_id", nullable = false)
    private MedicalService medicalService;

    @Column(nullable = false)
    private boolean active = true;

    public SpecialistOfferedService() {
    }

    public SpecialistOfferedService(SpecialistProfile specialistProfile, MedicalService medicalService) {
        this.specialistProfile = specialistProfile;
        this.medicalService = medicalService;
    }

    public Long getId() {
        return id;
    }

    public SpecialistProfile getSpecialistProfile() {
        return specialistProfile;
    }

    public MedicalService getMedicalService() {
        return medicalService;
    }

    public boolean isActive() {
        return active;
    }
}