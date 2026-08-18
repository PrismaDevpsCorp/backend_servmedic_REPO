package pe.prismadev.servmedic.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.prismadev.servmedic.dto.AdminPaymentResponse;
import pe.prismadev.servmedic.dto.AdminPaymentSummaryResponse;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class AdminPaymentService {

    private static final String DIRECT_EXTERNAL = "DIRECT_EXTERNAL";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminPaymentService(
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<AdminPaymentResponse> listPayments(
        String status
    ) {
        String normalizedStatus =
            normalizeStatus(status);

        String sql = """
            select
                mp.id as payment_id,
                mp.medical_request_id,
                mr.request_code,
                mr.status as request_status,

                mp.patient_profile_id,
                trim(
                    concat(
                        coalesce(pu.first_name, ''),
                        ' ',
                        coalesce(pu.last_name, '')
                    )
                ) as patient_full_name,
                pu.email as patient_email,
                pu.mobile_phone as patient_mobile_phone,

                mp.specialist_profile_id,
                trim(
                    concat(
                        coalesce(su.first_name, ''),
                        ' ',
                        coalesce(su.last_name, '')
                    )
                ) as specialist_full_name,
                su.email as specialist_email,
                su.mobile_phone as specialist_mobile_phone,

                mp.service_amount,
                mp.mobility_amount,
                mp.additional_amount,
                mp.amount,

                mp.platform_commission_percent,
                mp.platform_commission_amount,
                mp.specialist_net_amount,

                mp.currency,
                mp.payment_flow,
                mp.payment_method,
                mp.status,
                mp.external_transaction_id,

                mp.paid_at,
                mp.created_at

            from medical_payments mp

            join medical_requests mr
              on mr.id = mp.medical_request_id

            join patient_profiles pp
              on pp.id = mp.patient_profile_id

            join user_accounts pu
              on pu.id = pp.user_account_id

            join specialist_profiles sp
              on sp.id = mp.specialist_profile_id

            join user_accounts su
              on su.id = sp.user_account_id

            where mp.payment_flow = :paymentFlow
            """;

        MapSqlParameterSource params =
            new MapSqlParameterSource()
                .addValue(
                    "paymentFlow",
                    DIRECT_EXTERNAL
                );

        if (normalizedStatus != null) {

            sql =
                sql +
                " and mp.status = :status ";

            params.addValue(
                "status",
                normalizedStatus
            );
        }

        sql =
            sql +
            " order by mp.id desc ";

        return jdbcTemplate.query(
            sql,
            params,
            this::mapPaymentRow
        );
    }

    @Transactional(readOnly = true)
    public AdminPaymentSummaryResponse getSummary(
        String status
    ) {
        String normalizedStatus =
            normalizeStatus(status);

        String sql = """
            select
                count(*) as total_operations,

                count(*) filter (
                    where status = 'PAID'
                ) as paid_operations,

                count(*) filter (
                    where status = 'PENDING'
                ) as pending_operations,

                count(*) filter (
                    where status = 'REJECTED'
                ) as rejected_operations,

                coalesce(
                    sum(service_amount) filter (
                        where status = 'PAID'
                    ),
                    0
                ) as paid_service_amount,

                coalesce(
                    sum(mobility_amount) filter (
                        where status = 'PAID'
                    ),
                    0
                ) as paid_mobility_amount,

                coalesce(
                    sum(additional_amount) filter (
                        where status = 'PAID'
                    ),
                    0
                ) as paid_additional_amount,

                coalesce(
                    sum(amount) filter (
                        where status = 'PAID'
                    ),
                    0
                ) as paid_total_amount,

                coalesce(
                    sum(platform_commission_amount) filter (
                        where status = 'PAID'
                    ),
                    0
                ) as platform_commission_amount,

                coalesce(
                    sum(specialist_net_amount) filter (
                        where status = 'PAID'
                    ),
                    0
                ) as specialist_net_amount

            from medical_payments

            where payment_flow = :paymentFlow
            """;

        MapSqlParameterSource params =
            new MapSqlParameterSource()
                .addValue(
                    "paymentFlow",
                    DIRECT_EXTERNAL
                );

        if (normalizedStatus != null) {

            sql =
                sql +
                " and status = :status ";

            params.addValue(
                "status",
                normalizedStatus
            );
        }

        return jdbcTemplate.queryForObject(
            sql,
            params,
            this::mapSummaryRow
        );
    }

    private String normalizeStatus(
        String status
    ) {
        if (
            status == null ||
            status.isBlank()
        ) {
            return null;
        }

        return status
            .trim()
            .toUpperCase();
    }

    private AdminPaymentResponse mapPaymentRow(
        ResultSet rs,
        int rowNum
    ) throws SQLException {

        return new AdminPaymentResponse(
            getNullableLong(
                rs,
                "payment_id"
            ),
            getNullableLong(
                rs,
                "medical_request_id"
            ),
            getNullableString(
                rs,
                "request_code"
            ),
            getNullableString(
                rs,
                "request_status"
            ),

            getNullableLong(
                rs,
                "patient_profile_id"
            ),
            getNullableString(
                rs,
                "patient_full_name"
            ),
            getNullableString(
                rs,
                "patient_email"
            ),
            getNullableString(
                rs,
                "patient_mobile_phone"
            ),

            getNullableLong(
                rs,
                "specialist_profile_id"
            ),
            getNullableString(
                rs,
                "specialist_full_name"
            ),
            getNullableString(
                rs,
                "specialist_email"
            ),
            getNullableString(
                rs,
                "specialist_mobile_phone"
            ),

            getNullableBigDecimal(
                rs,
                "service_amount"
            ),
            getNullableBigDecimal(
                rs,
                "mobility_amount"
            ),
            getNullableBigDecimal(
                rs,
                "additional_amount"
            ),
            getNullableBigDecimal(
                rs,
                "amount"
            ),

            getNullableBigDecimal(
                rs,
                "platform_commission_percent"
            ),
            getNullableBigDecimal(
                rs,
                "platform_commission_amount"
            ),
            getNullableBigDecimal(
                rs,
                "specialist_net_amount"
            ),

            getNullableString(
                rs,
                "currency"
            ),
            getNullableString(
                rs,
                "payment_flow"
            ),
            getNullableString(
                rs,
                "payment_method"
            ),
            getNullableString(
                rs,
                "status"
            ),
            getNullableString(
                rs,
                "external_transaction_id"
            ),

            getTimestampAsIso(
                rs,
                "paid_at"
            ),
            getTimestampAsIso(
                rs,
                "created_at"
            )
        );
    }

    private AdminPaymentSummaryResponse mapSummaryRow(
        ResultSet rs,
        int rowNum
    ) throws SQLException {

        return new AdminPaymentSummaryResponse(
            getNullableLong(
                rs,
                "total_operations"
            ),
            getNullableLong(
                rs,
                "paid_operations"
            ),
            getNullableLong(
                rs,
                "pending_operations"
            ),
            getNullableLong(
                rs,
                "rejected_operations"
            ),

            getNullableBigDecimal(
                rs,
                "paid_service_amount"
            ),
            getNullableBigDecimal(
                rs,
                "paid_mobility_amount"
            ),
            getNullableBigDecimal(
                rs,
                "paid_additional_amount"
            ),
            getNullableBigDecimal(
                rs,
                "paid_total_amount"
            ),

            getNullableBigDecimal(
                rs,
                "platform_commission_amount"
            ),
            getNullableBigDecimal(
                rs,
                "specialist_net_amount"
            )
        );
    }

    private Long getNullableLong(
        ResultSet rs,
        String column
    ) throws SQLException {

        Object value =
            rs.getObject(column);

        if (value == null) {
            return null;
        }

        return ((Number) value)
            .longValue();
    }

    private BigDecimal getNullableBigDecimal(
        ResultSet rs,
        String column
    ) throws SQLException {

        return rs.getBigDecimal(column);
    }

    private String getNullableString(
        ResultSet rs,
        String column
    ) throws SQLException {

        String value =
            rs.getString(column);

        if (
            value == null ||
            value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private String getTimestampAsIso(
        ResultSet rs,
        String column
    ) throws SQLException {

        Timestamp timestamp =
            rs.getTimestamp(column);

        if (timestamp == null) {
            return null;
        }

        return timestamp
            .toInstant()
            .toString();
    }
}