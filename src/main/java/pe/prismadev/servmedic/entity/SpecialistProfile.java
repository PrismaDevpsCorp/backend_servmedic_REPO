package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "specialist_profiles")
public class SpecialistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profession_id", nullable = false)
    private Profession profession;

    @Column(name = "college_number", nullable = false, length = 50)
    private String collegeNumber;

    @Column(nullable = false, length = 50)
    private String status = "PENDING_VALIDATION";

    @Column(nullable = false)
    private boolean available = false;

    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount = 0;

    public SpecialistProfile() {
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public Profession getProfession() {
        return profession;
    }

    public void setProfession(Profession profession) {
        this.profession = profession;
    }

    public String getCollegeNumber() {
        return collegeNumber;
    }

    public void setCollegeNumber(String collegeNumber) {
        this.collegeNumber = collegeNumber;
    }

    public String getStatus() {
        return status;
    }

    public void activateForDevelopment() {
        this.status = "ACTIVE";
        this.available = true;
    }

    public void updateStatusFromAdmin(String newStatus) {
        this.status = newStatus;
        this.available = "ACTIVE".equals(newStatus);
    }

    public boolean isAvailable() {
        return available;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void applyNewPatientRating(int score) {
        BigDecimal currentTotal = ratingAverage.multiply(BigDecimal.valueOf(ratingCount));
        BigDecimal newTotal = currentTotal.add(BigDecimal.valueOf(score));
        int newCount = ratingCount + 1;

        this.ratingAverage = newTotal
            .divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

        this.ratingCount = newCount;
    }
}