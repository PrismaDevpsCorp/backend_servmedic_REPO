package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.prismadev.servmedic.dto.AdminLoginRequest;
import pe.prismadev.servmedic.dto.AdminLoginResponse;
import pe.prismadev.servmedic.service.AdminAuthService;

@RestController
@RequestMapping("/api/public/auth/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public AdminLoginResponse login(@RequestBody AdminLoginRequest request) {
        return adminAuthService.login(request);
    }
}