package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "specialist_wallet_transactions")
public class SpecialistWalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_profile_id", nullable = false)
    private SpecialistProfile specialistProfile;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_payment_id", nullable = false, unique = true)
    private MedicalPayment medicalPayment;

    @Column(name = "transaction_type", nullable = false, length = 40)
    private String transactionType = "SERVICE_PAYMENT";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "PEN";

    @Column(nullable = false, length = 40)
    private String status = "AVAILABLE";

    @Column(length = 250)
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public SpecialistWalletTransaction() {
    }

    public SpecialistWalletTransaction(SpecialistProfile specialistProfile, MedicalPayment medicalPayment, BigDecimal amount, String description) {
        this.specialistProfile = specialistProfile;
        this.medicalPayment = medicalPayment;
        this.amount = amount;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public SpecialistProfile getSpecialistProfile() {
        return specialistProfile;
    }

    public MedicalPayment getMedicalPayment() {
        return medicalPayment;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}