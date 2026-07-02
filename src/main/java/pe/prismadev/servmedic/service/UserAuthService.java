package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.UserLoginRequest;
import pe.prismadev.servmedic.dto.UserLoginResponse;
import pe.prismadev.servmedic.security.JwtService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class UserAuthService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserAuthService(
        NamedParameterJdbcTemplate jdbcTemplate,
        JwtService jwtService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public UserLoginResponse loginPatient(UserLoginRequest request) {
        return loginByRole(request, "PACIENTE");
    }

    @Transactional(readOnly = true)
    public UserLoginResponse loginSpecialist(UserLoginRequest request) {
        return loginByRole(request, "ESPECIALISTA");
    }

    private UserLoginResponse loginByRole(UserLoginRequest request, String expectedRole) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario requerido.");
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrasena requerida.");
        }

        String login = request.username().trim().toLowerCase();

        String sql = """
            select
                ua.id as user_id,
                ua.email,
                trim(concat(coalesce(ua.first_name, ''), ' ', coalesce(ua.last_name, ''))) as full_name,
                ua.password_hash,
                r.code as role_code,
                pp.id as patient_profile_id,
                sp.id as specialist_profile_id,
                sp.status as specialist_status
            from user_accounts ua
            join roles r on r.id = ua.role_id
            left join patient_profiles pp on pp.user_account_id = ua.id
            left join specialist_profiles sp on sp.user_account_id = ua.id
            where ua.active = true
            and r.active = true
            and r.code = :expectedRole
            and (
                lower(ua.email) = :login
                or ua.dni = :login
            )
            order by ua.id asc
            limit 1
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("login", login)
            .addValue("expectedRole", expectedRole);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);

        if (rows.isEmpty()) {
            throwUnauthorized();
        }

        Map<String, Object> row = rows.get(0);

        String passwordHash = getNullableString(row.get("password_hash"));

        if (passwordHash == null || !passwordEncoder.matches(request.password(), passwordHash)) {
            throwUnauthorized();
        }

        Long userId = getLong(row.get("user_id"));
        String email = getNullableString(row.get("email"));
        String fullName = getNullableString(row.get("full_name"));
        String role = getNullableString(row.get("role_code"));
        Long patientProfileId = getLong(row.get("patient_profile_id"));
        Long specialistProfileId = getLong(row.get("specialist_profile_id"));
        String specialistStatus = getNullableString(row.get("specialist_status"));

        if ("PACIENTE".equals(expectedRole) && patientProfileId == null) {
            throwUnauthorized();
        }

        if ("ESPECIALISTA".equals(expectedRole) && specialistProfileId == null) {
            throwUnauthorized();
        }

        String token = jwtService.generateUserToken(
            userId,
            email,
            fullName,
            role,
            patientProfileId,
            specialistProfileId,
            specialistStatus
        );

        return new UserLoginResponse(
            userId,
            email,
            fullName,
            role,
            patientProfileId,
            specialistProfileId,
            specialistStatus,
            token,
            Instant.now().toString()
        );
    }

    private void throwUnauthorized() {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Credenciales no validas."
        );
    }

    private Long getLong(Object value) {
        if (value == null) {
            return null;
        }

        return ((Number) value).longValue();
    }

    private String getNullableString(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString();

        if (text.isBlank()) {
            return null;
        }

        return text.trim();
    }
}