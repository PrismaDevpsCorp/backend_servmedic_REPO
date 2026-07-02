package pe.prismadev.servmedic.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "firebase_uid", length = 150, unique = true)
    private String firebaseUid;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @Column(name = "mobile_phone", length = 20)
    private String mobilePhone;

    @Column(name = "landline_phone", length = 20)
    private String landlinePhone;

    @Column(name = "address_text", nullable = false, length = 250)
    private String addressText;

    @Column(name = "address_reference", length = 250)
    private String addressReference;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    private boolean active = true;

    public UserAccount() {
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getLandlinePhone() {
        return landlinePhone;
    }

    public void setLandlinePhone(String landlinePhone) {
        this.landlinePhone = landlinePhone;
    }

    public String getAddressText() {
        return addressText;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public String getAddressReference() {
        return addressReference;
    }

    public void setAddressReference(String addressReference) {
        this.addressReference = addressReference;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public boolean isActive() {
        return active;
    }
}