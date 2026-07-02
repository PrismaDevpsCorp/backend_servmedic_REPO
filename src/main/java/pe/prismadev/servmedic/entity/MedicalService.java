package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "medical_services")
public class MedicalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profession_id", nullable = false)
    private Profession profession;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 220)
    private String name;

    @Column(name = "requires_prescription", nullable = false)
    private boolean requiresPrescription;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    public MedicalService() {
    }

    public Long getId() {
        return id;
    }

    public Profession getProfession() {
        return profession;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isRequiresPrescription() {
        return requiresPrescription;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }
}