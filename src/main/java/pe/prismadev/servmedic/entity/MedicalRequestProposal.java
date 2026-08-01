package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "medical_request_proposals")
public class MedicalRequestProposal {

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

    @Column(
        name = "service_amount",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal serviceAmount;

    @Column(
        name = "mobility_policy_snapshot",
        nullable = false,
        length = 30
    )
    private String mobilityPolicySnapshot;

    @Column(
        name = "mobility_amount",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal mobilityAmount = BigDecimal.ZERO;

    @Column(
        name = "total_amount",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
        nullable = false,
        length = 10
    )
    private String currency = "PEN";

    @Column(
        name = "proposal_message",
        length = 500
    )
    private String proposalMessage;

    @Column(
        name = "estimated_arrival_minutes",
        nullable = false
    )
    private Integer estimatedArrivalMinutes;

    @Column(
        nullable = false,
        length = 30
    )
    private String status = "PENDING";

    @Column(
        name = "expires_at",
        nullable = false
    )
    private OffsetDateTime expiresAt;

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
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "withdrawn_at")
    private OffsetDateTime withdrawnAt;

    public MedicalRequestProposal() {
    }

    public MedicalRequestProposal(
        MedicalRequest medicalRequest,
        SpecialistProfile specialistProfile,
        BigDecimal serviceAmount,
        String mobilityPolicySnapshot,
        BigDecimal mobilityAmount,
        BigDecimal totalAmount,
        String proposalMessage,
        Integer estimatedArrivalMinutes,
        OffsetDateTime expiresAt
    ) {
        this.medicalRequest = medicalRequest;
        this.specialistProfile = specialistProfile;
        this.serviceAmount = serviceAmount;
        this.mobilityPolicySnapshot = mobilityPolicySnapshot;
        this.mobilityAmount = mobilityAmount;
        this.totalAmount = totalAmount;
        this.proposalMessage = proposalMessage;
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
        this.expiresAt = expiresAt;
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

        if (mobilityAmount == null) {
            mobilityAmount = BigDecimal.ZERO;
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

    public BigDecimal getServiceAmount() {
        return serviceAmount;
    }

    public String getMobilityPolicySnapshot() {
        return mobilityPolicySnapshot;
    }

    public BigDecimal getMobilityAmount() {
        return mobilityAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProposalMessage() {
        return proposalMessage;
    }

    public Integer getEstimatedArrivalMinutes() {
        return estimatedArrivalMinutes;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
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

    public boolean isExpiredAt(OffsetDateTime referenceTime) {
        return expiresAt != null
            && !expiresAt.isAfter(referenceTime);
    }

    public void accept(OffsetDateTime responseTime) {
        requirePending("aceptar");
        this.status = "ACCEPTED";
        this.respondedAt = responseTime;
    }

    public void reject(OffsetDateTime responseTime) {
        requirePending("rechazar");
        this.status = "REJECTED";
        this.respondedAt = responseTime;
    }

    public void withdraw(OffsetDateTime withdrawalTime) {
        requirePending("retirar");
        this.status = "WITHDRAWN";
        this.withdrawnAt = withdrawalTime;
    }

    public void expire(OffsetDateTime expirationTime) {
        requirePending("marcar como expirada");
        this.status = "EXPIRED";
        this.respondedAt = expirationTime;
    }

    private void requirePending(String actionName) {
        if (!isPending()) {
            throw new IllegalStateException(
                "No se puede " + actionName
                    + " una propuesta en estado " + status + "."
            );
        }
    }
}
