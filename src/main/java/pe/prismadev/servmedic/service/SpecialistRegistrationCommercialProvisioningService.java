package pe.prismadev.servmedic.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecialistRegistrationCommercialProvisioningService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SpecialistRegistrationCommercialProvisioningService(
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void provision(
        Long specialistProfileId,
        String professionCode
    ) {
        if (specialistProfileId == null) {
            throw new IllegalArgumentException(
                "specialistProfileId es obligatorio."
            );
        }

        if (
            professionCode == null
                || professionCode.isBlank()
        ) {
            throw new IllegalArgumentException(
                "professionCode es obligatorio."
            );
        }

        MapSqlParameterSource params =
            new MapSqlParameterSource()
                .addValue(
                    "specialistProfileId",
                    specialistProfileId
                )
                .addValue(
                    "professionCode",
                    professionCode.trim()
                );

        provisionProfessionServices(params);
        provisionCommercialProfile(params);
        provisionInitialCash(params);
    }

    private void provisionProfessionServices(
        MapSqlParameterSource params
    ) {
        String sql = """
            INSERT INTO specialist_offered_services (
                specialist_profile_id,
                medical_service_id,
                base_price,
                active
            )
            SELECT
                :specialistProfileId,
                ms.id,
                CASE p.code
                    WHEN 'MEDICO_GENERAL' THEN 90.00
                    WHEN 'ENFERMERIA' THEN 70.00
                    WHEN 'TERAPIA_FISICA_REHABILITACION'
                        THEN 80.00
                    WHEN 'PSICOLOGIA' THEN 85.00
                    ELSE 75.00
                END,
                TRUE
            FROM medical_services ms
            JOIN professions p
              ON p.id = ms.profession_id
            WHERE p.code = :professionCode
              AND p.active = TRUE
              AND ms.active = TRUE
            ON CONFLICT (
                specialist_profile_id,
                medical_service_id
            )
            DO NOTHING
            """;

        jdbcTemplate.update(
            sql,
            params
        );
    }

    private void provisionCommercialProfile(
        MapSqlParameterSource params
    ) {
        String sql = """
            INSERT INTO specialist_commercial_profiles (
                specialist_profile_id
            )
            VALUES (
                :specialistProfileId
            )
            ON CONFLICT (
                specialist_profile_id
            )
            DO NOTHING
            """;

        jdbcTemplate.update(
            sql,
            params
        );
    }

    private void provisionInitialCash(
        MapSqlParameterSource params
    ) {
        String sql = """
            INSERT INTO specialist_payment_methods (
                specialist_profile_id,
                payment_method_id,
                active
            )
            SELECT
                :specialistProfileId,
                pm.id,
                TRUE
            FROM payment_methods pm
            WHERE pm.code = 'CASH'
              AND pm.active = TRUE
            ON CONFLICT (
                specialist_profile_id,
                payment_method_id
            )
            DO NOTHING
            """;

        jdbcTemplate.update(
            sql,
            params
        );
    }
}
