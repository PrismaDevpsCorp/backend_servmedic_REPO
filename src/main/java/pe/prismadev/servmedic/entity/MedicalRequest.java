package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "medical_requests")
public class MedicalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_code", nullable = false, unique = true, length = 50)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_profile_id", nullable = false)
    private PatientProfile patientProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_service_id", nullable = false)
    private MedicalService medicalService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_specialist_profile_id")
    private SpecialistProfile acceptedSpecialistProfile;

    @Column(nullable = false, length = 40)
    private String status = "PENDING";

    @Column(name = "address_text", nullable = false, length = 250)
    private String addressText;

    @Column(name = "address_reference", length = 250)
    private String addressReference;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "prescription_image_url", length = 500)
    private String prescriptionImageUrl;

    @Column(name = "patient_notes", columnDefinition = "TEXT")
    private String patientNotes;

    @Column(name = "estimated_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "started_route_at")
    private OffsetDateTime startedRouteAt;

    @Column(name = "started_attention_at")
    private OffsetDateTime startedAttentionAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    public MedicalRequest() {
    }

    public Long getId() {
        return id;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public PatientProfile getPatientProfile() {
        return patientProfile;
    }

    public void setPatientProfile(PatientProfile patientProfile) {
        this.patientProfile = patientProfile;
    }

    public MedicalService getMedicalService() {
        return medicalService;
    }

    public void setMedicalService(MedicalService medicalService) {
        this.medicalService = medicalService;
    }

    public SpecialistProfile getAcceptedSpecialistProfile() {
        return acceptedSpecialistProfile;
    }

    public String getStatus() {
        return status;
    }

    public void acceptBy(SpecialistProfile specialistProfile) {
        this.acceptedSpecialistProfile = specialistProfile;
        this.status = "ACCEPTED";
        this.acceptedAt = OffsetDateTime.now();
    }

    public void startRoute() {
        this.status = "EN_CAMINO";
        this.startedRouteAt = OffsetDateTime.now();
    }

    public void startAttention() {
        this.status = "EN_ATENCION";
        this.startedAttentionAt = OffsetDateTime.now();
    }

    public void finish() {
        this.status = "FINALIZADO";
        this.finishedAt = OffsetDateTime.now();
    }

    public String getAddressText() {
        return addressText;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public String getAddressReference() {
        return addressReference;
    }

    public void setAddressReference(String addressReference) {
        this.addressReference = addressReference;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getPrescriptionImageUrl() {
        return prescriptionImageUrl;
    }

    public void setPrescriptionImageUrl(String prescriptionImageUrl) {
        this.prescriptionImageUrl = prescriptionImageUrl;
    }

    public String getPatientNotes() {
        return patientNotes;
    }

    public void setPatientNotes(String patientNotes) {
        this.patientNotes = patientNotes;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public void setEstimatedAmount(BigDecimal estimatedAmount) {
        this.estimatedAmount = estimatedAmount;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public OffsetDateTime getStartedRouteAt() {
        return startedRouteAt;
    }

    public OffsetDateTime getStartedAttentionAt() {
        return startedAttentionAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }
}