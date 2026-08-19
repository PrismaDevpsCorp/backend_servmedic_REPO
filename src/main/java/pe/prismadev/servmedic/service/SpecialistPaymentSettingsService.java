package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.SpecialistPaymentSettingResponse;
import pe.prismadev.servmedic.dto.UpdateSpecialistPaymentSettingRequest;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SpecialistPaymentSettingsService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SpecialistPaymentSettingsService(
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<SpecialistPaymentSettingResponse> getSettings(
        Long specialistProfileId
    ) {
        requireSpecialistProfileId(
            specialistProfileId
        );

        String sql = """
            SELECT
                pm.id AS payment_method_id,
                pm.code,
                pm.name,
                pm.requires_voucher,
                COALESCE(spm.active, FALSE) AS selected,
                spm.mobile_phone,
                spm.account_holder,
                spm.bank_name,
                spm.account_number,
                spm.cci
            FROM payment_methods pm
            LEFT JOIN specialist_payment_methods spm
              ON spm.payment_method_id = pm.id
             AND spm.specialist_profile_id =
                 :specialistProfileId
            WHERE pm.active = TRUE
            ORDER BY pm.id ASC
            """;

        MapSqlParameterSource params =
            new MapSqlParameterSource()
                .addValue(
                    "specialistProfileId",
                    specialistProfileId
                );

        return jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) ->
                new SpecialistPaymentSettingResponse(
                    rs.getLong("payment_method_id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getBoolean("requires_voucher"),
                    rs.getBoolean("selected"),
                    rs.getString("mobile_phone"),
                    rs.getString("account_holder"),
                    rs.getString("bank_name"),
                    rs.getString("account_number"),
                    rs.getString("cci")
                )
        );
    }

    @Transactional
    public List<SpecialistPaymentSettingResponse> updateSettings(
        Long specialistProfileId,
        List<UpdateSpecialistPaymentSettingRequest> settings
    ) {
        requireSpecialistProfileId(
            specialistProfileId
        );

        Map<String, Long> availableMethods =
            loadAvailableMethods();

        if (
            settings == null
                || settings.size() != availableMethods.size()
        ) {
            throw badRequest(
                "Debe enviar todos los metodos de pago disponibles."
            );
        }

        Set<String> receivedCodes =
            new HashSet<>();

        int selectedCount = 0;

        for (
            UpdateSpecialistPaymentSettingRequest setting :
            settings
        ) {

            if (setting == null) {
                throw badRequest(
                    "La configuracion de pago no puede ser nula."
                );
            }

            String code =
                normalizeCode(
                    setting.code()
                );

            if (!availableMethods.containsKey(code)) {
                throw badRequest(
                    "Metodo de pago no disponible: "
                        + code
                );
            }

            if (!receivedCodes.add(code)) {
                throw badRequest(
                    "Metodo de pago repetido: "
                        + code
                );
            }

            validateSelected(
                code,
                setting
            );

            if (setting.selected()) {
                selectedCount++;
            }
        }

        if (selectedCount < 1) {
            throw badRequest(
                "Debe mantener al menos un metodo de pago habilitado."
            );
        }

        for (
            UpdateSpecialistPaymentSettingRequest setting :
            settings
        ) {

            String code =
                normalizeCode(
                    setting.code()
                );

            Long paymentMethodId =
                availableMethods.get(code);

            if (setting.selected()) {

                saveSelected(
                    specialistProfileId,
                    paymentMethodId,
                    code,
                    setting
                );
            }
            else {

                disablePreservingDetails(
                    specialistProfileId,
                    paymentMethodId
                );
            }
        }

        return getSettings(
            specialistProfileId
        );
    }

    private Map<String, Long> loadAvailableMethods() {

        String sql = """
            SELECT
                id,
                code
            FROM payment_methods
            WHERE active = TRUE
            ORDER BY id ASC
            """;

        List<Map<String, Object>> rows =
            jdbcTemplate.queryForList(
                sql,
                new MapSqlParameterSource()
            );

        Map<String, Long> result =
            new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {

            Object idValue =
                row.get("id");

            Object codeValue =
                row.get("code");

            if (
                !(idValue instanceof Number id)
                    || codeValue == null
            ) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Catalogo de metodos de pago invalido."
                );
            }

            result.put(
                normalizeCode(
                    codeValue.toString()
                ),
                id.longValue()
            );
        }

        if (result.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No existen metodos de pago activos."
            );
        }

        return result;
    }

    private void validateSelected(
        String code,
        UpdateSpecialistPaymentSettingRequest setting
    ) {
        if (!setting.selected()) {
            return;
        }

        switch (code) {

            case "CASH" -> {
                return;
            }

            case "YAPE", "PLIN" -> {

                requireText(
                    setting.mobilePhone(),
                    30,
                    "El numero de celular es obligatorio para "
                        + code + "."
                );

                requireText(
                    setting.accountHolder(),
                    160,
                    "El titular es obligatorio para "
                        + code + "."
                );
            }

            case "TRANSFER" -> {

                requireText(
                    setting.bankName(),
                    120,
                    "El banco es obligatorio para transferencia."
                );

                requireText(
                    setting.accountHolder(),
                    160,
                    "El titular es obligatorio para transferencia."
                );

                requireText(
                    setting.accountNumber(),
                    80,
                    "El numero de cuenta es obligatorio para transferencia."
                );

                String cci =
                    requireText(
                        setting.cci(),
                        40,
                        "El CCI es obligatorio para transferencia."
                    );

                if (!cci.matches("\\d{20}")) {
                    throw badRequest(
                        "El CCI debe contener exactamente 20 digitos."
                    );
                }
            }

            default ->
                throw badRequest(
                    "Metodo de pago no soportado: "
                        + code
                );
        }
    }

    private void saveSelected(
        Long specialistProfileId,
        Long paymentMethodId,
        String code,
        UpdateSpecialistPaymentSettingRequest setting
    ) {
        String mobilePhone = null;
        String accountHolder = null;
        String bankName = null;
        String accountNumber = null;
        String cci = null;

        if (
            "YAPE".equals(code)
                || "PLIN".equals(code)
        ) {

            mobilePhone =
                cleanNullable(
                    setting.mobilePhone()
                );

            accountHolder =
                cleanNullable(
                    setting.accountHolder()
                );
        }
        else if ("TRANSFER".equals(code)) {

            accountHolder =
                cleanNullable(
                    setting.accountHolder()
                );

            bankName =
                cleanNullable(
                    setting.bankName()
                );

            accountNumber =
                cleanNullable(
                    setting.accountNumber()
                );

            cci =
                cleanNullable(
                    setting.cci()
                );
        }

        String sql = """
            INSERT INTO specialist_payment_methods (
                specialist_profile_id,
                payment_method_id,
                active,
                mobile_phone,
                account_holder,
                bank_name,
                account_number,
                cci
            )
            VALUES (
                :specialistProfileId,
                :paymentMethodId,
                TRUE,
                :mobilePhone,
                :accountHolder,
                :bankName,
                :accountNumber,
                :cci
            )
            ON CONFLICT (
                specialist_profile_id,
                payment_method_id
            )
            DO UPDATE SET
                active = TRUE,
                mobile_phone = EXCLUDED.mobile_phone,
                account_holder = EXCLUDED.account_holder,
                bank_name = EXCLUDED.bank_name,
                account_number = EXCLUDED.account_number,
                cci = EXCLUDED.cci
            """;

        MapSqlParameterSource params =
            new MapSqlParameterSource()
                .addValue(
                    "specialistProfileId",
                    specialistProfileId
                )
                .addValue(
                    "paymentMethodId",
                    paymentMethodId
                )
                .addValue(
                    "mobilePhone",
                    mobilePhone
                )
                .addValue(
                    "accountHolder",
                    accountHolder
                )
                .addValue(
                    "bankName",
                    bankName
                )
                .addValue(
                    "accountNumber",
                    accountNumber
                )
                .addValue(
                    "cci",
                    cci
                );

        jdbcTemplate.update(
            sql,
            params
        );
    }

    private void disablePreservingDetails(
        Long specialistProfileId,
        Long paymentMethodId
    ) {
        MapSqlParameterSource params =
            new MapSqlParameterSource()
                .addValue(
                    "specialistProfileId",
                    specialistProfileId
                )
                .addValue(
                    "paymentMethodId",
                    paymentMethodId
                );

        String updateSql = """
            UPDATE specialist_payment_methods
            SET active = FALSE
            WHERE specialist_profile_id =
                :specialistProfileId
              AND payment_method_id =
                :paymentMethodId
            """;

        int affected =
            jdbcTemplate.update(
                updateSql,
                params
            );

        if (affected > 0) {
            return;
        }

        String insertSql = """
            INSERT INTO specialist_payment_methods (
                specialist_profile_id,
                payment_method_id,
                active
            )
            VALUES (
                :specialistProfileId,
                :paymentMethodId,
                FALSE
            )
            ON CONFLICT (
                specialist_profile_id,
                payment_method_id
            )
            DO NOTHING
            """;

        jdbcTemplate.update(
            insertSql,
            params
        );
    }

    private void requireSpecialistProfileId(
        Long specialistProfileId
    ) {
        if (specialistProfileId == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se pudo identificar al especialista."
            );
        }
    }

    private String normalizeCode(
        String value
    ) {
        if (
            value == null
                || value.isBlank()
        ) {
            throw badRequest(
                "El codigo del metodo de pago es obligatorio."
            );
        }

        return value
            .trim()
            .toUpperCase(
                Locale.ROOT
            );
    }

    private String requireText(
        String value,
        int maxLength,
        String message
    ) {
        String cleaned =
            cleanNullable(
                value
            );

        if (cleaned == null) {
            throw badRequest(
                message
            );
        }

        if (cleaned.length() > maxLength) {
            throw badRequest(
                message
                    + " Longitud maxima: "
                    + maxLength
                    + "."
            );
        }

        return cleaned;
    }

    private String cleanNullable(
        String value
    ) {
        if (
            value == null
                || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private ResponseStatusException badRequest(
        String message
    ) {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            message
        );
    }
}
