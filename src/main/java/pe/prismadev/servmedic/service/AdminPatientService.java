package pe.prismadev.servmedic.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.prismadev.servmedic.dto.AdminPatientResponse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class AdminPatientService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminPatientService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<AdminPatientResponse> listPatients(String search) {
        String normalizedSearch = normalizeSearch(search);

        String sql = """
            select
                pp.id as patient_profile_id,
                ua.id as user_id,
                trim(concat(coalesce(ua.first_name, ''), ' ', coalesce(ua.last_name, ''))) as full_name,
                ua.email,
                ua.dni,
                ua.mobile_phone,
                count(mr_count.id) as total_medical_requests,
                last_mr.request_code as last_request_code,
                last_mr.status as last_request_status,
                last_mr.created_at as last_request_created_at
            from patient_profiles pp
            join user_accounts ua on ua.id = pp.user_account_id
            left join medical_requests mr_count on mr_count.patient_profile_id = pp.id
            left join lateral (
                select
                    mr2.request_code,
                    mr2.status,
                    mr2.created_at
                from medical_requests mr2
                where mr2.patient_profile_id = pp.id
                order by mr2.created_at desc, mr2.id desc
                limit 1
            ) last_mr on true
            """;

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (normalizedSearch != null) {
            sql = sql + """
                where
                    lower(trim(concat(coalesce(ua.first_name, ''), ' ', coalesce(ua.last_name, '')))) like :search
                    or lower(coalesce(ua.email, '')) like :search
                    or lower(coalesce(ua.dni, '')) like :search
                    or lower(coalesce(ua.mobile_phone, '')) like :search
                """;

            params.addValue("search", "%" + normalizedSearch + "%");
        }

        sql = sql + """
            group by
                pp.id,
                ua.id,
                ua.first_name,
                ua.last_name,
                ua.email,
                ua.dni,
                ua.mobile_phone,
                last_mr.request_code,
                last_mr.status,
                last_mr.created_at
            order by pp.id desc
            """;

        return jdbcTemplate.query(sql, params, this::mapRow);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        return search.trim().toLowerCase();
    }

    private AdminPatientResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AdminPatientResponse(
            getNullableLong(rs, "patient_profile_id"),
            getNullableLong(rs, "user_id"),
            getNullableString(rs, "full_name"),
            getNullableString(rs, "email"),
            getNullableString(rs, "dni"),
            getNullableString(rs, "mobile_phone"),
            getNullableLong(rs, "total_medical_requests"),
            getNullableString(rs, "last_request_code"),
            getNullableString(rs, "last_request_status"),
            getTimestampAsIso(rs, "last_request_created_at")
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);

        if (value == null) {
            return null;
        }

        return ((Number) value).longValue();
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