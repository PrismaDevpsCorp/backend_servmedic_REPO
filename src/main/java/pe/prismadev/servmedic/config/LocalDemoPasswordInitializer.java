package pe.prismadev.servmedic.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalDemoPasswordInitializer {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LocalDemoPasswordInitializer(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeDemoPasswords() {
        setPasswordIfEmpty("paciente.demo@correo.com", "PacienteServMedic2026");
        setPasswordIfEmpty("medico.demo@correo.com", "EspecialistaServMedic2026");
    }

    private void setPasswordIfEmpty(String email, String rawPassword) {
        String passwordHash = passwordEncoder.encode(rawPassword);

        String sql = """
            update user_accounts
            set password_hash = :passwordHash
            where lower(email) = lower(:email)
            and (
                password_hash is null
                or trim(password_hash) = ''
            )
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("email", email)
            .addValue("passwordHash", passwordHash);

        jdbcTemplate.update(sql, params);
    }
}