package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.UserLoginRequest;
import pe.prismadev.servmedic.dto.UserLoginResponse;
import pe.prismadev.servmedic.service.UserAuthService;

@RestController
@RequestMapping("/api/public/auth")
public class UserAuthController {

    private final UserAuthService userAuthService;

    public UserAuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/patient/login")
    public UserLoginResponse loginPatient(@RequestBody UserLoginRequest request) {
        return userAuthService.loginPatient(request);
    }

    @PostMapping("/specialist/login")
    public UserLoginResponse loginSpecialist(@RequestBody UserLoginRequest request) {
        return userAuthService.loginSpecialist(request);
    }
}