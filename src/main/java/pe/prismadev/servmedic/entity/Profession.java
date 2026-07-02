package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "professions")
public class Profession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "college_acronym", nullable = false, length = 20)
    private String collegeAcronym;

    @Column(nullable = false)
    private boolean active;

    public Profession() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCollegeAcronym() {
        return collegeAcronym;
    }

    public boolean isActive() {
        return active;
    }
}