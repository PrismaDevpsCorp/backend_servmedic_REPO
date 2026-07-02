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

    @Column(nullable = false, length = 40)
    private String status = "PAID";

    @Column(name = "external_transaction_id", length = 120)
    private String externalTransactionId;

    @Column(name = "paid_at", insertable = false, updatable = false)
    private OffsetDateTime paidAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public MedicalPayment() {
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPlatformCommissionPercent() {
        return platformCommissionPercent;
    }

    public void setPlatformCommissionPercent(BigDecimal platformCommissionPercent) {
        this.platformCommissionPercent = platformCommissionPercent;
    }

    public BigDecimal getPlatformCommissionAmount() {
        return platformCommissionAmount;
    }

    public void setPlatformCommissionAmount(BigDecimal platformCommissionAmount) {
        this.platformCommissionAmount = platformCommissionAmount;
    }

    public BigDecimal getSpecialistNetAmount() {
        return specialistNetAmount;
    }

    public void setSpecialistNetAmount(BigDecimal specialistNetAmount) {
        this.specialistNetAmount = specialistNetAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public void setExternalTransactionId(String externalTransactionId) {
        this.externalTransactionId = externalTransactionId;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}