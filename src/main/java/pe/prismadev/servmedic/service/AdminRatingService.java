package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.AdminRatingResponse;
import pe.prismadev.servmedic.dto.AdminRatingSummaryResponse;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class AdminRatingService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminRatingService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<AdminRatingResponse> listRatings(String ratingType) {
        String normalizedRaterRole = normalizeRatingType(ratingType);

        String sql = """
            select
                sr.id as rating_id,
                sr.medical_request_id,
                mr.request_code,
                mr.status as request_status,

                sr.rater_role,

                sr.patient_profile_id,
                trim(concat(coalesce(pu.first_name, ''), ' ', coalesce(pu.last_name, ''))) as patient_full_name,
                pu.email as patient_email,
                pu.mobile_phone as patient_mobile_phone,

                sr.specialist_profile_id,
                trim(concat(coalesce(su.first_name, ''), ' ', coalesce(su.last_name, ''))) as specialist_full_name,
                su.email as specialist_email,
                su.mobile_phone as specialist_mobile_phone,

                sr.score,
                sr.comment,
                sr.created_at
            from service_ratings sr
            join medical_requests mr on mr.id = sr.medical_request_id
            join patient_profiles pp on pp.id = sr.patient_profile_id
            join user_accounts pu on pu.id = pp.user_account_id
            join specialist_profiles sp on sp.id = sr.specialist_profile_id
            join user_accounts su on su.id = sp.user_account_id
            """;

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (normalizedRaterRole != null) {
            sql = sql + " where sr.rater_role = :raterRole ";
            params.addValue("raterRole", normalizedRaterRole);
        }

        sql = sql + " order by sr.id desc ";

        return jdbcTemplate.query(sql, params, this::mapRatingRow);
    }

    @Transactional(readOnly = true)
    public AdminRatingSummaryResponse getSummary(String ratingType) {
        String normalizedRaterRole = normalizeRatingType(ratingType);

        String sql = """
            select
                count(sr.id) as total_ratings,
                coalesce(avg(sr.score), 0) as average_score,

                count(sr.id) filter (where sr.rater_role = 'PATIENT') as patient_to_specialist_ratings,
                count(sr.id) filter (where sr.rater_role = 'SPECIALIST') as specialist_to_patient_ratings,

                coalesce(avg(sr.score) filter (where sr.rater_role = 'PATIENT'), 0) as average_patient_to_specialist_score,
                coalesce(avg(sr.score) filter (where sr.rater_role = 'SPECIALIST'), 0) as average_specialist_to_patient_score
            from service_ratings sr
            """;

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (normalizedRaterRole != null) {
            sql = sql + " where sr.rater_role = :raterRole ";
            params.addValue("raterRole", normalizedRaterRole);
        }

        return jdbcTemplate.queryForObject(sql, params, this::mapSummaryRow);
    }

    private String normalizeRatingType(String ratingType) {
        if (ratingType == null || ratingType.isBlank()) {
            return null;
        }

        String normalized = ratingType.trim().toUpperCase();

        return switch (normalized) {
            case "PATIENT", "PATIENT_TO_SPECIALIST" -> "PATIENT";
            case "SPECIALIST", "SPECIALIST_TO_PATIENT" -> "SPECIALIST";
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tipo de calificacion no permitido: " + ratingType
            );
        };
    }

    private AdminRatingResponse mapRatingRow(ResultSet rs, int rowNum) throws SQLException {
        String raterRole = getNullableString(rs, "rater_role");

        return new AdminRatingResponse(
            getNullableLong(rs, "rating_id"),
            getNullableLong(rs, "medical_request_id"),
            getNullableString(rs, "request_code"),
            getNullableString(rs, "request_status"),

            raterRole,
            getRatingTypeLabel(raterRole),

            getNullableLong(rs, "patient_profile_id"),
            getNullableString(rs, "patient_full_name"),
            getNullableString(rs, "patient_email"),
            getNullableString(rs, "patient_mobile_phone"),

            getNullableLong(rs, "specialist_profile_id"),
            getNullableString(rs, "specialist_full_name"),
            getNullableString(rs, "specialist_email"),
            getNullableString(rs, "specialist_mobile_phone"),

            getNullableInteger(rs, "score"),
            getNullableString(rs, "comment"),
            getTimestampAsIso(rs, "created_at")
        );
    }

    private AdminRatingSummaryResponse mapSummaryRow(ResultSet rs, int rowNum) throws SQLException {
        return new AdminRatingSummaryResponse(
            getNullableLong(rs, "total_ratings"),
            getNullableBigDecimal(rs, "average_score"),
            getNullableLong(rs, "patient_to_specialist_ratings"),
            getNullableLong(rs, "specialist_to_patient_ratings"),
            getNullableBigDecimal(rs, "average_patient_to_specialist_score"),
            getNullableBigDecimal(rs, "average_specialist_to_patient_score")
        );
    }

    private String getRatingTypeLabel(String raterRole) {
        if ("PATIENT".equals(raterRole)) {
            return "PATIENT_TO_SPECIALIST";
        }

        if ("SPECIALIST".equals(raterRole)) {
            return "SPECIALIST_TO_PATIENT";
        }

        return raterRole;
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);

        if (value == null) {
            return null;
        }

        return ((Number) value).longValue();
    }

    private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);

        if (value == null) {
            return null;
        }

        return ((Number) value).intValue();
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