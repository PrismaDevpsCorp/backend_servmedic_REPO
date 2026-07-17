package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "specialist_commercial_profiles")
public class SpecialistCommercialProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "specialist_profile_id",
        nullable = false,
        unique = true
    )
    private SpecialistProfile specialistProfile;

    @Column(name = "mobility_policy", nullable = false, length = 30)
    private String mobilityPolicy = "INCLUDED";

    @Column(
        name = "mobility_reference_amount",
        precision = 10,
        scale = 2
    )
    private BigDecimal mobilityReferenceAmount;

    @Column(name = "commercial_notes", length = 500)
    private String commercialNotes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(
        name = "created_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public SpecialistCommercialProfile() {
    }

    public SpecialistCommercialProfile(SpecialistProfile specialistProfile) {
        this.specialistProfile = specialistProfile;
    }

    @PrePersist
    public void beforeInsert() {
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public SpecialistProfile getSpecialistProfile() {
        return specialistProfile;
    }

    public String getMobilityPolicy() {
        return mobilityPolicy;
    }

    public BigDecimal getMobilityReferenceAmount() {
        return mobilityReferenceAmount;
    }

    public String getCommercialNotes() {
        return commercialNotes;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateCommercialData(
        String mobilityPolicy,
        BigDecimal mobilityReferenceAmount,
        String commercialNotes
    ) {
        this.mobilityPolicy = mobilityPolicy;
        this.mobilityReferenceAmount = mobilityReferenceAmount;
        this.commercialNotes = commercialNotes;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}