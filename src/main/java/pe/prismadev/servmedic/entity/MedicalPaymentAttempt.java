package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "medical_payment_attempts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_medical_payment_attempt_number",
            columnNames = {
                "medical_payment_id",
                "attempt_number"
            }
        )
    }
)
public class MedicalPaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_payment_id", nullable = false)
    private MedicalPayment medicalPayment;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "payment_method", nullable = false, length = 40)
    private String paymentMethod;

    @Column(name = "external_transaction_id", length = 120)
    private String externalTransactionId;

    @Column(name = "evidence_file_name", nullable = false, length = 255)
    private String evidenceFileName;

    @Column(name = "evidence_content_type", nullable = false, length = 100)
    private String evidenceContentType;

    @Column(name = "evidence_size", nullable = false)
    private Long evidenceSize;

    @Column(name = "evidence_data", nullable = false, columnDefinition = "bytea")
    private byte[] evidenceData;

    @Column(name = "evidence_uploaded_at", nullable = false)
    private OffsetDateTime evidenceUploadedAt;

    @Column(name = "rejected_at", nullable = false)
    private OffsetDateTime rejectedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "rejected_by_specialist_profile_id",
        nullable = false
    )
    private SpecialistProfile rejectedBySpecialistProfile;

    @Column(name = "rejection_reason", nullable = false, length = 500)
    private String rejectionReason;

    @Column(
        name = "archived_at",
        insertable = false,
        updatable = false
    )
    private OffsetDateTime archivedAt;

    public MedicalPaymentAttempt() {
    }

    public Long getId() {
        return id;
    }

    public MedicalPayment getMedicalPayment() {
        return medicalPayment;
    }

    public void setMedicalPayment(MedicalPayment value) {
        this.medicalPayment = value;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer value) {
        this.attemptNumber = value;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String value) {
        this.paymentMethod = value;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public void setExternalTransactionId(String value) {
        this.externalTransactionId = value;
    }

    public String getEvidenceFileName() {
        return evidenceFileName;
    }

    public void setEvidenceFileName(String value) {
        this.evidenceFileName = value;
    }

    public String getEvidenceContentType() {
        return evidenceContentType;
    }

    public void setEvidenceContentType(String value) {
        this.evidenceContentType = value;
    }

    public Long getEvidenceSize() {
        return evidenceSize;
    }

    public void setEvidenceSize(Long value) {
        this.evidenceSize = value;
    }

    public byte[] getEvidenceData() {
        return evidenceData;
    }

    public void setEvidenceData(byte[] value) {
        this.evidenceData = value;
    }

    public OffsetDateTime getEvidenceUploadedAt() {
        return evidenceUploadedAt;
    }

    public void setEvidenceUploadedAt(OffsetDateTime value) {
        this.evidenceUploadedAt = value;
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

    public OffsetDateTime getArchivedAt() {
        return archivedAt;
    }
}
