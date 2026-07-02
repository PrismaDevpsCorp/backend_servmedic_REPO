package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "service_ratings")
public class ServiceRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_request_id", nullable = false)
    private MedicalRequest medicalRequest;

    @Column(name = "rater_role", nullable = false, length = 30)
    private String raterRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_profile_id", nullable = false)
    private PatientProfile patientProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_profile_id", nullable = false)
    private SpecialistProfile specialistProfile;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ServiceRating() {
    }

    public Long getId() {
        return id;
    }

    public MedicalRequest getMedicalRequest() {
        return medicalRequest;
    }

    public void setMedicalRequest(MedicalRequest medicalRequest) {
        this.medicalRequest = medicalRequest;
    }

    public String getRaterRole() {
        return raterRole;
    }

    public void setRaterRole(String raterRole) {
        this.raterRole = raterRole;
    }

    public PatientProfile getPatientProfile() {
        return patientProfile;
    }

    public void setPatientProfile(PatientProfile patientProfile) {
        this.patientProfile = patientProfile;
    }

    public SpecialistProfile getSpecialistProfile() {
        return specialistProfile;
    }

    public void setSpecialistProfile(SpecialistProfile specialistProfile) {
        this.specialistProfile = specialistProfile;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}