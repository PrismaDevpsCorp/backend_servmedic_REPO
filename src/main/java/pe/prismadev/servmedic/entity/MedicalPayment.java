package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "medical_payments")
public class MedicalPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_request_id", nullable = false, unique = true)
    private MedicalRequest medicalRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_profile_id", nullable = false)
    private PatientProfile patientProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_profile_id", nullable = false)
    private SpecialistProfile specialistProfile;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "service_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal serviceAmount;

    @Column(name = "mobility_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal mobilityAmount;

    @Column(name = "additional_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalAmount = BigDecimal.ZERO;

    @Column(name = "platform_commission_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal platformCommissionPercent;

    @Column(name = "platform_commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformCommissionAmount;

    @Column(name = "specialist_net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal specialistNetAmount;

    @Column(nullable = false, length = 10)
    private String currency = "PEN";

    @Column(name = "payment_method", nullable = false, length = 40)
    private String paymentMethod;

    @Column(name = "payment_flow", nullable = false, length = 40)
    private String paymentFlow = "LEGACY";

    @Column(nullable = false, length = 40)
    private String status = "PENDING";

    @Column(name = "external_transaction_id", length = 120)
    private String externalTransactionId;

    @Column(name = "evidence_file_name", length = 255)
    private String evidenceFileName;

    @Column(name = "evidence_content_type", length = 100)
    private String evidenceContentType;

    @Column(name = "evidence_size")
    private Long evidenceSize;

    @Column(name = "evidence_data", columnDefinition = "bytea")
    private byte[] evidenceData;

    @Column(name = "evidence_uploaded_at")
    private OffsetDateTime evidenceUploadedAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_specialist_profile_id")
    private SpecialistProfile verifiedBySpecialistProfile;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_specialist_profile_id")
    private SpecialistProfile rejectedBySpecialistProfile;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "verification_warning_acknowledged", nullable = false)
    private boolean verificationWarningAcknowledged;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public MedicalPayment() {
    }

    public Long getId() { return id; }

    public MedicalRequest getMedicalRequest() { return medicalRequest; }
    public void setMedicalRequest(MedicalRequest value) { this.medicalRequest = value; }

    public PatientProfile getPatientProfile() { return patientProfile; }
    public void setPatientProfile(PatientProfile value) { this.patientProfile = value; }

    public SpecialistProfile getSpecialistProfile() { return specialistProfile; }
    public void setSpecialistProfile(SpecialistProfile value) { this.specialistProfile = value; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }

    public BigDecimal getServiceAmount() { return serviceAmount; }
    public void setServiceAmount(BigDecimal value) { this.serviceAmount = value; }

    public BigDecimal getMobilityAmount() { return mobilityAmount; }
    public void setMobilityAmount(BigDecimal value) { this.mobilityAmount = value; }

    public BigDecimal getAdditionalAmount() { return additionalAmount; }
    public void setAdditionalAmount(BigDecimal value) { this.additionalAmount = value; }

    public BigDecimal getPlatformCommissionPercent() { return platformCommissionPercent; }
    public void setPlatformCommissionPercent(BigDecimal value) { this.platformCommissionPercent = value; }

    public BigDecimal getPlatformCommissionAmount() { return platformCommissionAmount; }
    public void setPlatformCommissionAmount(BigDecimal value) { this.platformCommissionAmount = value; }

    public BigDecimal getSpecialistNetAmount() { return specialistNetAmount; }
    public void setSpecialistNetAmount(BigDecimal value) { this.specialistNetAmount = value; }

    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { this.paymentMethod = value; }

    public String getPaymentFlow() { return paymentFlow; }
    public void setPaymentFlow(String value) { this.paymentFlow = value; }

    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }

    public String getExternalTransactionId() { return externalTransactionId; }
    public void setExternalTransactionId(String value) { this.externalTransactionId = value; }

    public String getEvidenceFileName() { return evidenceFileName; }
    public void setEvidenceFileName(String value) { this.evidenceFileName = value; }

    public String getEvidenceContentType() { return evidenceContentType; }
    public void setEvidenceContentType(String value) { this.evidenceContentType = value; }

    public Long getEvidenceSize() { return evidenceSize; }
    public void setEvidenceSize(Long value) { this.evidenceSize = value; }

    public byte[] getEvidenceData() { return evidenceData; }
    public void setEvidenceData(byte[] value) { this.evidenceData = value; }

    public OffsetDateTime getEvidenceUploadedAt() { return evidenceUploadedAt; }
    public void setEvidenceUploadedAt(OffsetDateTime value) { this.evidenceUploadedAt = value; }

    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime value) { this.paidAt = value; }

    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(OffsetDateTime value) { this.verifiedAt = value; }

    public SpecialistProfile getVerifiedBySpecialistProfile() {
        return verifiedBySpecialistProfile;
    }

    public void setVerifiedBySpecialistProfile(SpecialistProfile value) {
        this.verifiedBySpecialistProfile = value;
    }

    public OffsetDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(OffsetDateTime value) {
        this.rejectedAt = value;
    }

    public SpecialistProfile getRejectedBySpecialistProfile() {
        return rejectedBySpecialistProfile;
    }

    public void setRejectedBySpecialistProfile(
        SpecialistProfile value
    ) {
        this.rejectedBySpecialistProfile = value;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String value) {
        this.rejectionReason = value;
    }

    public boolean isVerificationWarningAcknowledged() {
        return verificationWarningAcknowledged;
    }

    public void setVerificationWarningAcknowledged(boolean value) {
        this.verificationWarningAcknowledged = value;
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }
}
