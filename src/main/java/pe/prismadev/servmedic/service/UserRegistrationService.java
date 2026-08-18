package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CreatePatientRequest;
import pe.prismadev.servmedic.dto.CreateSpecialistRequest;
import pe.prismadev.servmedic.dto.PatientRegistrationResponse;
import pe.prismadev.servmedic.dto.SpecialistRegistrationResponse;
import pe.prismadev.servmedic.entity.*;
import pe.prismadev.servmedic.repository.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserRegistrationService {

    private final RoleRepository roleRepository;
    private final ProfessionRepository professionRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final UserAccountRepository userAccountRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final SpecialistProfileRepository specialistProfileRepository;
    private final SpecialistOfferedServiceRepository specialistOfferedServiceRepository;

    private final BCryptPasswordEncoder passwordEncoder =
        new BCryptPasswordEncoder();

    public UserRegistrationService(
        RoleRepository roleRepository,
        ProfessionRepository professionRepository,
        MedicalServiceRepository medicalServiceRepository,
        UserAccountRepository userAccountRepository,
        PatientProfileRepository patientProfileRepository,
        SpecialistProfileRepository specialistProfileRepository,
        SpecialistOfferedServiceRepository specialistOfferedServiceRepository
    ) {
        this.roleRepository = roleRepository;
        this.professionRepository = professionRepository;
        this.medicalServiceRepository = medicalServiceRepository;
        this.userAccountRepository = userAccountRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.specialistProfileRepository = specialistProfileRepository;
        this.specialistOfferedServiceRepository = specialistOfferedServiceRepository;
    }

    @Transactional
    public PatientRegistrationResponse registerPatient(
        CreatePatientRequest request
    ) {
        validatePassword(request.password());
        validateContact(
            request.mobilePhone(),
            request.landlinePhone()
        );

        String normalizedEmail =
            normalizeEmail(request.email());

        String normalizedDni =
            normalizeDni(request.dni());

        validateUniqueUser(
            normalizedEmail,
            normalizedDni
        );

        Role patientRole =
            findRole("PACIENTE");

        UserAccount user =
            new UserAccount();

        user.setRole(patientRole);

        user.setPasswordHash(
            passwordEncoder.encode(
                request.password()
            )
        );

        fillCommonUserData(
            user,
            normalizedEmail,
            request.firstName(),
            request.lastName(),
            normalizedDni,
            request.mobilePhone(),
            request.landlinePhone(),
            request.addressText(),
            request.addressReference(),
            request.latitude(),
            request.longitude()
        );

        UserAccount savedUser =
            userAccountRepository.save(user);

        PatientProfile patientProfile =
            new PatientProfile();

        patientProfile.setUserAccount(
            savedUser
        );

        patientProfile.setBloodType(
            cleanNullable(
                request.bloodType()
            )
        );

        patientProfile.setAllergies(
            cleanNullable(
                request.allergies()
            )
        );

        patientProfile.setPreexistingConditions(
            cleanNullable(
                request.preexistingConditions()
            )
        );

        PatientProfile savedProfile =
            patientProfileRepository.save(
                patientProfile
            );

        return new PatientRegistrationResponse(
            savedUser.getId(),
            savedProfile.getId(),
            patientRole.getCode(),
            savedUser.getEmail(),
            fullName(savedUser),
            savedUser.getDni(),
            "Paciente registrado correctamente. Ya puede iniciar sesion."
        );
    }

    @Transactional
    public SpecialistRegistrationResponse registerSpecialist(
        CreateSpecialistRequest request
    ) {
        validatePassword(request.password());

        validateContact(
            request.mobilePhone(),
            request.landlinePhone()
        );

        validateNoDuplicateServiceCodes(
            request.offeredServiceCodes()
        );

        String normalizedEmail =
            normalizeEmail(request.email());

        String normalizedDni =
            normalizeDni(request.dni());

        String professionCode =
            request.professionCode()
                .trim()
                .toUpperCase();

        validateUniqueUser(
            normalizedEmail,
            normalizedDni
        );

        Role specialistRole =
            findRole("ESPECIALISTA");

        Profession profession =
            professionRepository
                .findByCode(professionCode)
                .filter(Profession::isActive)
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Profesion no encontrada o inactiva: "
                                + professionCode
                        )
                );

        List<MedicalService> offeredServices =
            medicalServiceRepository
                .findActiveByCodes(
                    request.offeredServiceCodes()
                );

        if (
            offeredServices.size()
                != request.offeredServiceCodes().size()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Uno o mas servicios medicos no existen o estan inactivos."
            );
        }

        for (
            MedicalService service :
            offeredServices
        ) {
            if (
                !service
                    .getProfession()
                    .getCode()
                    .equals(
                        profession.getCode()
                    )
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Regla antintrusismo: el servicio "
                        + service.getCode()
                        + " no pertenece a la profesion "
                        + profession.getCode()
                );
            }
        }

        UserAccount user =
            new UserAccount();

        user.setRole(
            specialistRole
        );

        user.setPasswordHash(
            passwordEncoder.encode(
                request.password()
            )
        );

        fillCommonUserData(
            user,
            normalizedEmail,
            request.firstName(),
            request.lastName(),
            normalizedDni,
            request.mobilePhone(),
            request.landlinePhone(),
            request.addressText(),
            request.addressReference(),
            request.latitude(),
            request.longitude()
        );

        UserAccount savedUser =
            userAccountRepository.save(user);

        SpecialistProfile specialistProfile =
            new SpecialistProfile();

        specialistProfile.setUserAccount(
            savedUser
        );

        specialistProfile.setProfession(
            profession
        );

        specialistProfile.setCollegeNumber(
            request
                .collegeNumber()
                .trim()
        );

        SpecialistProfile savedProfile =
            specialistProfileRepository.save(
                specialistProfile
            );

        for (
            MedicalService service :
            offeredServices
        ) {
            specialistOfferedServiceRepository.save(
                new SpecialistOfferedService(
                    savedProfile,
                    service
                )
            );
        }

        return new SpecialistRegistrationResponse(
            savedUser.getId(),
            savedProfile.getId(),
            specialistRole.getCode(),
            profession.getCode(),
            savedProfile.getCollegeNumber(),
            savedProfile.getStatus(),
            savedUser.getEmail(),
            fullName(savedUser),
            savedUser.getDni(),
            request.offeredServiceCodes(),
            "Especialista registrado correctamente. "
                + "Estado inicial: PENDING_VALIDATION. "
                + "El acceso operativo se habilitara luego de la validacion administrativa."
        );
    }

    @Transactional
    public SpecialistRegistrationResponse activateSpecialistForDevelopment(
        Long specialistProfileId
    ) {
        SpecialistProfile profile =
            specialistProfileRepository
                .findById(
                    specialistProfileId
                )
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Perfil de especialista no encontrado: "
                                + specialistProfileId
                        )
                );

        profile.activateForDevelopment();

        SpecialistProfile saved =
            specialistProfileRepository.save(
                profile
            );

        UserAccount user =
            saved.getUserAccount();

        Profession profession =
            saved.getProfession();

        return new SpecialistRegistrationResponse(
            user.getId(),
            saved.getId(),
            user.getRole().getCode(),
            profession.getCode(),
            saved.getCollegeNumber(),
            saved.getStatus(),
            user.getEmail(),
            fullName(user),
            user.getDni(),
            List.of(),
            "Especialista activado temporalmente para pruebas de desarrollo."
        );
    }

    private Role findRole(
        String code
    ) {
        return roleRepository
            .findByCode(code)
            .filter(Role::isActive)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Rol no encontrado o inactivo: "
                            + code
                    )
            );
    }

    private void validateUniqueUser(
        String email,
        String dni
    ) {
        if (
            userAccountRepository
                .existsByEmail(email)
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe un usuario con el email: "
                    + email
            );
        }

        if (
            userAccountRepository
                .existsByDni(dni)
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe un usuario con el DNI: "
                    + dni
            );
        }
    }

    private void validateContact(
        String mobilePhone,
        String landlinePhone
    ) {
        boolean hasMobile =
            mobilePhone != null
                && !mobilePhone.isBlank();

        boolean hasLandline =
            landlinePhone != null
                && !landlinePhone.isBlank();

        if (
            !hasMobile
                && !hasLandline
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debe ingresar al menos celular o telefono."
            );
        }
    }

    private void validatePassword(
        String password
    ) {
        if (
            password == null
                || password.length() < 8
                || password.length() > 72
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La contrasena debe tener entre 8 y 72 caracteres."
            );
        }

        boolean hasLower =
            password.matches(
                ".*[a-z].*"
            );

        boolean hasUpper =
            password.matches(
                ".*[A-Z].*"
            );

        boolean hasDigit =
            password.matches(
                ".*[0-9].*"
            );

        if (
            !hasLower
                || !hasUpper
                || !hasDigit
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La contrasena debe contener mayuscula, minuscula y numero."
            );
        }
    }

    private void validateNoDuplicateServiceCodes(
        List<String> serviceCodes
    ) {
        Set<String> unique =
            new HashSet<>(
                serviceCodes
            );

        if (
            unique.size()
                != serviceCodes.size()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No debe repetir servicios ofrecidos."
            );
        }
    }

    private void fillCommonUserData(
        UserAccount user,
        String email,
        String firstName,
        String lastName,
        String dni,
        String mobilePhone,
        String landlinePhone,
        String addressText,
        String addressReference,
        java.math.BigDecimal latitude,
        java.math.BigDecimal longitude
    ) {
        user.setEmail(
            email
        );

        user.setFirstName(
            firstName.trim()
        );

        user.setLastName(
            lastName.trim()
        );

        user.setDni(
            dni
        );

        user.setMobilePhone(
            cleanNullable(
                mobilePhone
            )
        );

        user.setLandlinePhone(
            cleanNullable(
                landlinePhone
            )
        );

        user.setAddressText(
            addressText.trim()
        );

        user.setAddressReference(
            cleanNullable(
                addressReference
            )
        );

        user.setLatitude(
            latitude
        );

        user.setLongitude(
            longitude
        );
    }

    private String normalizeEmail(
        String value
    ) {
        return value
            .trim()
            .toLowerCase();
    }

    private String normalizeDni(
        String value
    ) {
        return value.trim();
    }

    private String cleanNullable(
        String value
    ) {
        return value == null
            || value.isBlank()
                ? null
                : value.trim();
    }

    private String fullName(
        UserAccount user
    ) {
        return user.getFirstName()
            + " "
            + user.getLastName();
    }
}