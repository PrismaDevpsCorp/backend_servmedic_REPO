package pe.prismadev.servmedic.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.SpecialistFinancialDashboardResponse;
import pe.prismadev.servmedic.entity.MedicalPayment;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.MedicalService;
import pe.prismadev.servmedic.entity.PatientProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalPaymentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SpecialistFinancialDashboardService {

    private static final BigDecimal COMMISSION_PERCENT =
        new BigDecimal("5.00");

    private static final int MAX_PAGE_SIZE = 100;

    private final MedicalPaymentRepository
        medicalPaymentRepository;

    public SpecialistFinancialDashboardService(
        MedicalPaymentRepository medicalPaymentRepository
    ) {
        this.medicalPaymentRepository =
            medicalPaymentRepository;
    }

    @Transactional(readOnly = true)
    public SpecialistFinancialDashboardResponse find(
        Long specialistProfileId,
        int page,
        int size
    ) {
        validateArguments(
            specialistProfileId,
            page,
            size
        );

        MedicalPaymentRepository
            .SpecialistFinancialSummaryProjection projection =
                medicalPaymentRepository
                    .summarizeDirectPaymentsForSpecialist(
                        specialistProfileId
                    );

        Page<MedicalPayment> paymentPage =
            medicalPaymentRepository
                .findBySpecialistProfile_IdAndPaymentFlow(
                    specialistProfileId,
                    "DIRECT_EXTERNAL",
                    PageRequest.of(
                        page,
                        size,
                        Sort.by(
                            Sort.Direction.DESC,
                            "id"
                        )
                    )
                );

        SpecialistFinancialDashboardResponse.Summary summary =
            toSummary(projection);

        List<
            SpecialistFinancialDashboardResponse.Operation
        > operations =
            paymentPage
                .getContent()
                .stream()
                .map(this::toOperation)
                .toList();

        return new SpecialistFinancialDashboardResponse(
            summary,
            operations,
            paymentPage.getNumber(),
            paymentPage.getSize(),
            paymentPage.getTotalElements(),
            paymentPage.getTotalPages()
        );
    }

    private void validateArguments(
        Long specialistProfileId,
        int page,
        int size
    ) {
        if (specialistProfileId == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se pudo identificar al especialista."
            );
        }

        if (page < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pagina no puede ser negativa."
            );
        }

        if (
            size < 1
                || size > MAX_PAGE_SIZE
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El tamano de pagina debe estar entre 1 y "
                    + MAX_PAGE_SIZE
                    + "."
            );
        }
    }

    private SpecialistFinancialDashboardResponse.Summary
    toSummary(
        MedicalPaymentRepository
            .SpecialistFinancialSummaryProjection projection
    ) {
        if (projection == null) {
            return emptySummary();
        }

        return new SpecialistFinancialDashboardResponse.Summary(
            longValue(
                projection.getTotalOperations()
            ),
            longValue(
                projection.getPaidOperations()
            ),
            longValue(
                projection.getPendingOperations()
            ),
            longValue(
                projection.getRejectedOperations()
            ),
            money(
                projection.getPaidServiceAmount()
            ),
            money(
                projection.getPaidMobilityAmount()
            ),
            money(
                projection.getPaidAdditionalAmount()
            ),
            money(
                projection.getPaidTotalAmount()
            ),
            COMMISSION_PERCENT,
            money(
                projection.getPlatformCommissionAmount()
            ),
            money(
                projection.getSpecialistNetAmount()
            ),
            "PEN"
        );
    }

    private SpecialistFinancialDashboardResponse.Summary
    emptySummary() {
        BigDecimal zero =
            BigDecimal.ZERO.setScale(
                2,
                RoundingMode.HALF_UP
            );

        return new SpecialistFinancialDashboardResponse.Summary(
            0L,
            0L,
            0L,
            0L,
            zero,
            zero,
            zero,
            zero,
            COMMISSION_PERCENT,
            zero,
            zero,
            "PEN"
        );
    }

    private SpecialistFinancialDashboardResponse.Operation
    toOperation(
        MedicalPayment payment
    ) {
        MedicalRequest medicalRequest =
            payment.getMedicalRequest();

        MedicalService medicalService =
            medicalRequest == null
                ? null
                : medicalRequest.getMedicalService();

        PatientProfile patientProfile =
            payment.getPatientProfile();

        UserAccount patientUser =
            patientProfile == null
                ? null
                : patientProfile.getUserAccount();

        String patientFullName =
            buildFullName(patientUser);

        String serviceName =
            medicalService == null
                ? ""
                : clean(
                    medicalService.getName()
                );

        return new SpecialistFinancialDashboardResponse.Operation(
            payment.getId(),
            medicalRequest == null
                ? null
                : medicalRequest.getId(),
            medicalRequest == null
                ? ""
                : clean(
                    medicalRequest.getRequestCode()
                ),
            serviceName,
            patientFullName,
            clean(payment.getStatus()),
            clean(payment.getPaymentMethod()),
            money(payment.getServiceAmount()),
            money(payment.getMobilityAmount()),
            money(payment.getAdditionalAmount()),
            money(payment.getAmount()),
            money(payment.getPlatformCommissionPercent()),
            money(payment.getPlatformCommissionAmount()),
            money(payment.getSpecialistNetAmount()),
            clean(payment.getCurrency()),
            cleanNullable(
                payment.getExternalTransactionId()
            ),
            payment.getPaidAt(),
            payment.getCreatedAt()
        );
    }

    private String buildFullName(
        UserAccount user
    ) {
        if (user == null) {
            return "";
        }

        String firstName =
            clean(user.getFirstName());

        String lastName =
            clean(user.getLastName());

        return (
            firstName +
            " " +
            lastName
        ).trim();
    }

    private BigDecimal money(
        BigDecimal value
    ) {
        return (
            value == null
                ? BigDecimal.ZERO
                : value
        ).setScale(
            2,
            RoundingMode.HALF_UP
        );
    }

    private long longValue(
        Long value
    ) {
        return value == null
            ? 0L
            : value;
    }

    private String clean(
        String value
    ) {
        return value == null
            ? ""
            : value.trim();
    }

    private String cleanNullable(
        String value
    ) {
        return value == null
                || value.isBlank()
            ? null
            : value.trim();
    }
}
