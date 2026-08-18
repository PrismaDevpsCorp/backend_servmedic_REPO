package pe.prismadev.servmedic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.SpecialistFinancialDashboardResponse;
import pe.prismadev.servmedic.entity.MedicalPayment;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.MedicalService;
import pe.prismadev.servmedic.entity.PatientProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalPaymentRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialistFinancialDashboardServiceTest {

    private static final Long SPECIALIST_ID =
        7L;

    @Mock
    private MedicalPaymentRepository
        medicalPaymentRepository;

    @Mock
    private MedicalPaymentRepository
        .SpecialistFinancialSummaryProjection
        summaryProjection;

    private SpecialistFinancialDashboardService
        service;

    @BeforeEach
    void setUp() {
        service =
            new SpecialistFinancialDashboardService(
                medicalPaymentRepository
            );
    }

    @Test
    void dashboardUsesOnlyPaidAmountsInSummaryAndListsAllStates() {

        when(
            summaryProjection.getTotalOperations()
        ).thenReturn(3L);

        when(
            summaryProjection.getPaidOperations()
        ).thenReturn(1L);

        when(
            summaryProjection.getPendingOperations()
        ).thenReturn(1L);

        when(
            summaryProjection.getRejectedOperations()
        ).thenReturn(1L);

        when(
            summaryProjection.getPaidServiceAmount()
        ).thenReturn(
            new BigDecimal("100.00")
        );

        when(
            summaryProjection.getPaidMobilityAmount()
        ).thenReturn(
            new BigDecimal("20.00")
        );

        when(
            summaryProjection.getPaidAdditionalAmount()
        ).thenReturn(
            new BigDecimal("30.00")
        );

        when(
            summaryProjection.getPaidTotalAmount()
        ).thenReturn(
            new BigDecimal("150.00")
        );

        when(
            summaryProjection
                .getPlatformCommissionAmount()
        ).thenReturn(
            new BigDecimal("5.00")
        );

        when(
            summaryProjection
                .getSpecialistNetAmount()
        ).thenReturn(
            new BigDecimal("145.00")
        );

        when(
            medicalPaymentRepository
                .summarizeDirectPaymentsForSpecialist(
                    SPECIALIST_ID
                )
        ).thenReturn(
            summaryProjection
        );

        MedicalPayment paid =
            payment(
                30L,
                "SM-FIN-003",
                "PAID",
                "100.00",
                "20.00",
                "30.00",
                "150.00",
                "5.00",
                "145.00"
            );

        MedicalPayment pending =
            payment(
                29L,
                "SM-FIN-002",
                "PENDING",
                "90.00",
                "0.00",
                "25.00",
                "115.00",
                "0.00",
                "115.00"
            );

        MedicalPayment rejected =
            payment(
                28L,
                "SM-FIN-001",
                "REJECTED",
                "80.00",
                "10.00",
                "0.00",
                "90.00",
                "0.00",
                "90.00"
            );

        when(
            medicalPaymentRepository
                .findBySpecialistProfile_IdAndPaymentFlow(
                    eq(SPECIALIST_ID),
                    eq("DIRECT_EXTERNAL"),
                    any(Pageable.class)
                )
        ).thenReturn(
            new PageImpl<>(
                List.of(
                    paid,
                    pending,
                    rejected
                )
            )
        );

        SpecialistFinancialDashboardResponse result =
            service.find(
                SPECIALIST_ID,
                0,
                20
            );

        assertEquals(
            3L,
            result.summary().totalOperations()
        );

        assertEquals(
            1L,
            result.summary().paidOperations()
        );

        assertEquals(
            1L,
            result.summary().pendingOperations()
        );

        assertEquals(
            1L,
            result.summary().rejectedOperations()
        );

        assertEquals(
            new BigDecimal("100.00"),
            result.summary().paidServiceAmount()
        );

        assertEquals(
            new BigDecimal("20.00"),
            result.summary().paidMobilityAmount()
        );

        assertEquals(
            new BigDecimal("30.00"),
            result.summary().paidAdditionalAmount()
        );

        assertEquals(
            new BigDecimal("150.00"),
            result.summary().paidTotalAmount()
        );

        assertEquals(
            new BigDecimal("5.00"),
            result.summary()
                .platformCommissionPercent()
        );

        assertEquals(
            new BigDecimal("5.00"),
            result.summary()
                .platformCommissionAmount()
        );

        assertEquals(
            new BigDecimal("145.00"),
            result.summary()
                .specialistNetAmount()
        );

        assertEquals(
            3,
            result.operations().size()
        );

        assertEquals(
            "PAID",
            result.operations()
                .get(0)
                .paymentStatus()
        );

        assertEquals(
            "PENDING",
            result.operations()
                .get(1)
                .paymentStatus()
        );

        assertEquals(
            "REJECTED",
            result.operations()
                .get(2)
                .paymentStatus()
        );
    }

    @Test
    void rejectsInvalidPageSizeBeforeRepositoryAccess() {

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () ->
                    service.find(
                        SPECIALIST_ID,
                        0,
                        101
                    )
            );

        assertEquals(
            400,
            exception.getStatusCode().value()
        );
    }

    private MedicalPayment payment(
        Long id,
        String requestCode,
        String status,
        String serviceAmount,
        String mobilityAmount,
        String additionalAmount,
        String totalAmount,
        String commissionAmount,
        String specialistNet
    ) {
        MedicalPayment payment =
            new MedicalPayment();

        ReflectionTestUtils.setField(
            payment,
            "id",
            id
        );

        ReflectionTestUtils.setField(
            payment,
            "createdAt",
            OffsetDateTime.now()
        );

        MedicalRequest request =
            mock(MedicalRequest.class);

        MedicalService medicalService =
            mock(MedicalService.class);

        PatientProfile patient =
            mock(PatientProfile.class);

        UserAccount patientUser =
            mock(UserAccount.class);

        when(request.getId())
            .thenReturn(id + 100L);

        when(request.getRequestCode())
            .thenReturn(requestCode);

        when(request.getMedicalService())
            .thenReturn(medicalService);

        when(medicalService.getName())
            .thenReturn("Consulta medica");

        when(patient.getUserAccount())
            .thenReturn(patientUser);

        when(patientUser.getFirstName())
            .thenReturn("Paciente");

        when(patientUser.getLastName())
            .thenReturn("Demo");

        payment.setMedicalRequest(request);
        payment.setPatientProfile(patient);

        payment.setPaymentFlow(
            "DIRECT_EXTERNAL"
        );

        payment.setStatus(status);
        payment.setPaymentMethod("YAPE");

        payment.setServiceAmount(
            new BigDecimal(serviceAmount)
        );

        payment.setMobilityAmount(
            new BigDecimal(mobilityAmount)
        );

        payment.setAdditionalAmount(
            new BigDecimal(additionalAmount)
        );

        payment.setAmount(
            new BigDecimal(totalAmount)
        );

        payment.setPlatformCommissionPercent(
            new BigDecimal("5.00")
        );

        payment.setPlatformCommissionAmount(
            new BigDecimal(commissionAmount)
        );

        payment.setSpecialistNetAmount(
            new BigDecimal(specialistNet)
        );

        payment.setCurrency("PEN");

        payment.setExternalTransactionId(
            "REF-" + id
        );

        if ("PAID".equals(status)) {
            payment.setPaidAt(
                OffsetDateTime.now()
            );
        }

        return payment;
    }
}
