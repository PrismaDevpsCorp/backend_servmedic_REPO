package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.SpecialistRegistrationResponse;
import pe.prismadev.servmedic.service.UserRegistrationService;

@RestController
@RequestMapping("/api/dev/specialists")
public class DevSpecialistController {

    private final UserRegistrationService userRegistrationService;

    public DevSpecialistController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @PatchMapping("/{specialistProfileId}/activate")
    public SpecialistRegistrationResponse activateForDevelopment(@PathVariable Long specialistProfileId) {
        return userRegistrationService.activateSpecialistForDevelopment(specialistProfileId);
    }
}