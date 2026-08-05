package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Entity
@Table(name = "medical_request_additionals")
public class MedicalRequestAdditional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "medical_request_id",
        nullable = false
    )
    private MedicalRequest medicalRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "specialist_profile_id",
        nullable = false
    )
    private SpecialistProfile specialistProfile;

    @Column(nullable = false, length = 120)
    private String concept;

    @Column(nullable = false, length = 1000)
    private String justification;

    @Column(
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "PEN";

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private OffsetDateTime updatedAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "withdrawn_at")
    private OffsetDateTime withdrawnAt;

    public MedicalRequestAdditional() {
    }

    public MedicalRequestAdditional(
        MedicalRequest medicalRequest,
        SpecialistProfile specialistProfile,
        String concept,
        String justification,
        BigDecimal amount
    ) {
        this.medicalRequest = medicalRequest;
        this.specialistProfile = specialistProfile;
        this.concept = concept;
        this.justification = justification;
        this.amount = amount;
    }

    @PrePersist
    public void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();

        if (status == null || status.isBlank()) {
            status = "PENDING";
        }

        if (currency == null || currency.isBlank()) {
            currency = "PEN";
        }

        if (concept != null) {
            concept = concept.trim();
        }

        if (justification != null) {
            justification = justification.trim();
        }

        if (amount != null) {
            amount = amount.setScale(
                2,
                RoundingMode.HALF_UP
            );
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public MedicalRequest getMedicalRequest() {
        return medicalRequest;
    }

    public SpecialistProfile getSpecialistProfile() {
        return specialistProfile;
    }

    public String getConcept() {
        return concept;
    }

    public String getJustification() {
        return justification;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public OffsetDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public void approve(OffsetDateTime responseTime) {
        requirePending("aprobar");
        requireTime(responseTime);
        status = "APPROVED";
        respondedAt = responseTime;
    }

    public void reject(OffsetDateTime responseTime) {
        requirePending("rechazar");
        requireTime(responseTime);
        status = "REJECTED";
        respondedAt = responseTime;
    }

    public void withdraw(OffsetDateTime withdrawalTime) {
        requirePending("retirar");
        requireTime(withdrawalTime);
        status = "WITHDRAWN";
        withdrawnAt = withdrawalTime;
    }

    private void requirePending(String actionName) {
        if (!isPending()) {
            throw new IllegalStateException(
                "No se puede " + actionName
                    + " un adicional en estado " + status + "."
            );
        }
    }

    private void requireTime(OffsetDateTime transitionTime) {
        if (transitionTime == null) {
            throw new IllegalArgumentException(
                "La fecha de la decision es obligatoria."
            );
        }
    }
}
