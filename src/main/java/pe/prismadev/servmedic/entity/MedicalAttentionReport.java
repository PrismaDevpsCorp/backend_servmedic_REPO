package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "medical_attention_reports")
public class MedicalAttentionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_request_id", nullable = false, unique = true)
    private MedicalRequest medicalRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_profile_id", nullable = false)
    private SpecialistProfile specialistProfile;

    @Column(name = "clinical_observations", nullable = false, columnDefinition = "TEXT")
    private String clinicalObservations;

    @Column(name = "diagnostic_impression", columnDefinition = "TEXT")
    private String diagnosticImpression;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendations;

    @Column(columnDefinition = "TEXT")
    private String indications;

    @Column(name = "vital_signs", columnDefinition = "TEXT")
    private String vitalSigns;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public MedicalAttentionReport() {
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

    public SpecialistProfile getSpecialistProfile() {
        return specialistProfile;
    }

    public void setSpecialistProfile(SpecialistProfile specialistProfile) {
        this.specialistProfile = specialistProfile;
    }

    public String getClinicalObservations() {
        return clinicalObservations;
    }

    public void setClinicalObservations(String clinicalObservations) {
        this.clinicalObservations = clinicalObservations;
    }

    public String getDiagnosticImpression() {
        return diagnosticImpression;
    }

    public void setDiagnosticImpression(String diagnosticImpression) {
        this.diagnosticImpression = diagnosticImpression;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public String getIndications() {
        return indications;
    }

    public void setIndications(String indications) {
        this.indications = indications;
    }

    public String getVitalSigns() {
        return vitalSigns;
    }

    public void setVitalSigns(String vitalSigns) {
        this.vitalSigns = vitalSigns;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void touchUpdatedAt() {
        this.updatedAt = OffsetDateTime.now();
    }
}