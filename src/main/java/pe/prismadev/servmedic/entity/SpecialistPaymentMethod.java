package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "specialist_payment_methods")
public class SpecialistPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_profile_id", nullable = false)
    private SpecialistProfile specialistProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private boolean active = true;

    @Column(
        name = "created_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    public SpecialistPaymentMethod() {
    }

    public SpecialistPaymentMethod(
        SpecialistProfile specialistProfile,
        PaymentMethod paymentMethod
    ) {
        this.specialistProfile = specialistProfile;
        this.paymentMethod = paymentMethod;
    }

    public Long getId() {
        return id;
    }

    public SpecialistProfile getSpecialistProfile() {
        return specialistProfile;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}