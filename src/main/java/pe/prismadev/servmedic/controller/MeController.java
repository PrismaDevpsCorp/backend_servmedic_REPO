package pe.prismadev.servmedic.controller;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class MeController {

    @GetMapping("/api/admin/me")
    public Map<String, Object> adminMe(Authentication authentication) {
        return buildMeResponse(authentication, "ADMIN");
    }

    @GetMapping("/api/patient/me")
    public Map<String, Object> patientMe(Authentication authentication) {
        return buildMeResponse(authentication, "PACIENTE");
    }

    @GetMapping("/api/specialist/me")
    public Map<String, Object> specialistMe(Authentication authentication) {
        return buildMeResponse(authentication, "ESPECIALISTA");
    }

    private Map<String, Object> buildMeResponse(Authentication authentication, String expectedRole) {
        Claims claims = (Claims) authentication.getDetails();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authenticated", authentication.isAuthenticated());
        response.put("expectedRole", expectedRole);
        response.put("email", claims.get("email", String.class));
        response.put("fullName", claims.get("fullName", String.class));
        response.put("role", claims.get("role", String.class));
        response.put("userId", getLong(claims.get("userId")));
        response.put("patientProfileId", getLong(claims.get("patientProfileId")));
        response.put("specialistProfileId", getLong(claims.get("specialistProfileId")));
        response.put("specialistStatus", claims.get("specialistStatus", String.class));

        return response;
    }

    private Long getLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.valueOf(value.toString());
    }
}