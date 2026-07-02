package pe.prismadev.servmedic.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.prismadev.servmedic.dto.AdminMedicalRequestResponse;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class AdminMedicalRequestService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminMedicalRequestService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<AdminMedicalRequestResponse> listMedicalRequests(String status) {
        String normalizedStatus = normalizeStatus(status);

        String sql = """
            select
                mr.id as request_id,
                mr.request_code,
                mr.status,

                mr.patient_profile_id,
                trim(concat(coalesce(pu.first_name, ''), ' ', coalesce(pu.last_name, ''))) as patient_full_name,
                pu.mobile_phone as patient_mobile_phone,
                pu.email as patient_email,

                ms.code as service_code,
                ms.name as service_name,

                mr.accepted_specialist_profile_id as specialist_profile_id,
                nullif(trim(concat(coalesce(su.first_name, ''), ' ', coalesce(su.last_name, ''))), '') as specialist_full_name,
                su.mobile_phone as specialist_mobile_phone,

                mr.estimated_amount,
                mr.address_text,
                mr.address_reference,
                mr.latitude,
                mr.longitude,

                mp.status as payment_status,
                mp.amount as paid_amount,
                mp.platform_commission_amount,
                mp.specialist_net_amount,
                mp.payment_method,

                mr.created_at,
                mr.accepted_at,
                mr.started_route_at,
                mr.started_attention_at,
                mr.finished_at,
                mr.cancelled_at
            from medical_requests mr
            join patient_profiles pp on pp.id = mr.patient_profile_id
            join user_accounts pu on pu.id = pp.user_account_id
            join medical_services ms on ms.id = mr.medical_service_id
            left join specialist_profiles sp on sp.id = mr.accepted_specialist_profile_id
            left join user_accounts su on su.id = sp.user_account_id
            left join medical_payments mp on mp.medical_request_id = mr.id
            """;

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (normalizedStatus != null) {
            sql = sql + " where mr.status = :status ";
            params.addValue("status", normalizedStatus);
        }

        sql = sql + " order by mr.id desc ";

        return jdbcTemplate.query(sql, params, this::mapRow);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return status.trim().toUpperCase();
    }

    private AdminMedicalRequestResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AdminMedicalRequestResponse(
            getNullableLong(rs, "request_id"),
            getNullableString(rs, "request_code"),
            getNullableString(rs, "status"),

            getNullableLong(rs, "patient_profile_id"),
            getNullableString(rs, "patient_full_name"),
            getNullableString(rs, "patient_mobile_phone"),
            getNullableString(rs, "patient_email"),

            getNullableString(rs, "service_code"),
            getNullableString(rs, "service_name"),

            getNullableLong(rs, "specialist_profile_id"),
            getNullableString(rs, "specialist_full_name"),
            getNullableString(rs, "specialist_mobile_phone"),

            getNullableBigDecimal(rs, "estimated_amount"),
            getNullableString(rs, "address_text"),
            getNullableString(rs, "address_reference"),
            getNullableBigDecimal(rs, "latitude"),
            getNullableBigDecimal(rs, "longitude"),

            getNullableString(rs, "payment_status"),
            getNullableBigDecimal(rs, "paid_amount"),
            getNullableBigDecimal(rs, "platform_commission_amount"),
            getNullableBigDecimal(rs, "specialist_net_amount"),
            getNullableString(rs, "payment_method"),

            getTimestampAsIso(rs, "created_at"),
            getTimestampAsIso(rs, "accepted_at"),
            getTimestampAsIso(rs, "started_route_at"),
            getTimestampAsIso(rs, "started_attention_at"),
            getTimestampAsIso(rs, "finished_at"),
            getTimestampAsIso(rs, "cancelled_at")
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);

        if (value == null) {
            return null;
        }

        return ((Number) value).longValue();
    }

    private BigDecimal getNullableBigDecimal(ResultSet rs, String column) throws SQLException {
        return rs.getBigDecimal(column);
    }

    private String getNullableString(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String getTimestampAsIso(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);

        if (timestamp == null) {
            return null;
        }

        return timestamp.toInstant().toString();
    }
}