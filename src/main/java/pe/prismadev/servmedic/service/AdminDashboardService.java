package pe.prismadev.servmedic.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pe.prismadev.servmedic.dto.DashboardStatusCountResponse;
import pe.prismadev.servmedic.dto.DashboardSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminDashboardService {

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummaryResponse getSummary() {
        Long totalPatients = count("SELECT COUNT(*) FROM patient_profiles");

        Long totalSpecialists = count("SELECT COUNT(*) FROM specialist_profiles");
        Long activeSpecialists = count("SELECT COUNT(*) FROM specialist_profiles WHERE status = 'ACTIVE'");
        Long pendingValidationSpecialists = count("SELECT COUNT(*) FROM specialist_profiles WHERE status = 'PENDING_VALIDATION'");
        Long pendingInterviewSpecialists = count("SELECT COUNT(*) FROM specialist_profiles WHERE status = 'PENDING_INTERVIEW'");
        Long suspendedSpecialists = count("SELECT COUNT(*) FROM specialist_profiles WHERE status = 'SUSPENDED'");

        Long totalMedicalRequests = count("SELECT COUNT(*) FROM medical_requests");
        List<DashboardStatusCountResponse> medicalRequestsByStatus = getMedicalRequestsByStatus();

        Long totalPayments = count("SELECT COUNT(*) FROM medical_payments");
        BigDecimal grossAmount = money("SELECT COALESCE(SUM(amount), 0) FROM medical_payments");
        BigDecimal platformCommissionAmount = money("SELECT COALESCE(SUM(platform_commission_amount), 0) FROM medical_payments");
        BigDecimal specialistNetAmount = money("SELECT COALESCE(SUM(specialist_net_amount), 0) FROM medical_payments");

        BigDecimal specialistAvailableWalletBalance = money("""
            SELECT COALESCE(SUM(amount), 0)
            FROM specialist_wallet_transactions
            WHERE status = 'AVAILABLE'
        """);

        return new DashboardSummaryResponse(
            totalPatients,
            totalSpecialists,
            activeSpecialists,
            pendingValidationSpecialists,
            pendingInterviewSpecialists,
            suspendedSpecialists,
            totalMedicalRequests,
            medicalRequestsByStatus,
            totalPayments,
            grossAmount,
            platformCommissionAmount,
            specialistNetAmount,
            specialistAvailableWalletBalance,
            LocalDateTime.now()
        );
    }

    private List<DashboardStatusCountResponse> getMedicalRequestsByStatus() {
        String sql = """
            SELECT status, COUNT(*) AS total
            FROM medical_requests
            GROUP BY status
            ORDER BY status ASC
        """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new DashboardStatusCountResponse(
                rs.getString("status"),
                rs.getLong("total")
            )
        );
    }

    private Long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private BigDecimal money(String sql) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }
}