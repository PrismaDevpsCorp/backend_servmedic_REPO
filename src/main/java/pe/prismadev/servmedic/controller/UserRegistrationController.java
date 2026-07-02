package pe.prismadev.servmedic.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.CreatePatientRequest;
import pe.prismadev.servmedic.dto.CreateSpecialistRequest;
import pe.prismadev.servmedic.dto.PatientRegistrationResponse;
import pe.prismadev.servmedic.dto.SpecialistRegistrationResponse;
import pe.prismadev.servmedic.service.UserRegistrationService;

@RestController
@RequestMapping("/api/public/register")
public class UserRegistrationController {

    private final UserRegistrationService userRegistrationService;

    public UserRegistrationController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @PostMapping("/patient")
    public PatientRegistrationResponse registerPatient(@Valid @RequestBody CreatePatientRequest request) {
        return userRegistrationService.registerPatient(request);
    }

    @PostMapping("/specialist")
    public SpecialistRegistrationResponse registerSpecialist(@Valid @RequestBody CreateSpecialistRequest request) {
        return userRegistrationService.registerSpecialist(request);
    }
}