package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.AdminLoginRequest;
import pe.prismadev.servmedic.dto.AdminLoginResponse;
import pe.prismadev.servmedic.security.JwtService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AdminAuthService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthService(
        NamedParameterJdbcTemplate jdbcTemplate,
        JwtService jwtService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario requerido.");
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña requerida.");
        }

        String login = request.username().trim().toLowerCase();

        String sql = """
            select
                ua.id as user_id,
                ua.email,
                trim(concat(coalesce(ua.first_name, ''), ' ', coalesce(ua.last_name, ''))) as full_name,
                ua.password_hash,
                r.code as role_code
            from user_accounts ua
            join roles r on r.id = ua.role_id
            where ua.active = true
            and r.active = true
            and r.code = 'ADMIN'
            and (
                lower(ua.email) = :login
                or (:login = 'admin' and lower(ua.email) = lower('admin@servmedic.local'))
            )
            order by ua.id asc
            limit 1
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("login", login);

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

        String token = jwtService.generateAdminToken(
            userId,
            email,
            fullName,
            role
        );

        return new AdminLoginResponse(
            userId,
            email,
            fullName,
            role,
            token,
            Instant.now().toString()
        );
    }

    private void throwUnauthorized() {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Credenciales administrativas no validas."
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