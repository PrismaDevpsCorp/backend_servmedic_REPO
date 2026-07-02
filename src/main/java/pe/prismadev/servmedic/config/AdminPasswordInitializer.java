package pe.prismadev.servmedic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminPasswordInitializer implements ApplicationRunner {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${servmedic.admin.default-password:AdminServMedic2026}")
    private String defaultAdminPassword;

    public AdminPasswordInitializer(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (defaultAdminPassword == null || defaultAdminPassword.isBlank()) {
            return;
        }

        String passwordHash = passwordEncoder.encode(defaultAdminPassword);

        String sql = """
            update user_accounts ua
            set
                password_hash = :passwordHash,
                updated_at = now()
            from roles r
            where ua.role_id = r.id
            and r.code = 'ADMIN'
            and lower(ua.email) = lower('admin@servmedic.local')
            and (ua.password_hash is null or ua.password_hash = '')
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("passwordHash", passwordHash);

        jdbcTemplate.update(sql, params);
    }
}