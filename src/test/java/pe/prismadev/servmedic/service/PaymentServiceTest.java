package pe.prismadev.servmedic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.ManualPaymentResponse;
import pe.prismadev.servmedic.entity.MedicalPayment;
import pe.prismadev.servmedic.entity.MedicalPaymentAttempt;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.MedicalRequestProposal;
import pe.prismadev.servmedic.entity.PatientProfile;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalPaymentAttemptRepository;
import pe.prismadev.servmedic.repository.MedicalPaymentRepository;
import pe.prismadev.servmedic.repository.MedicalRequestAdditionalRepository;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    private static final Long REQUEST_ID = 16L;
    private static final Long PATIENT_ID = 1L;
    private static final Long SPECIALIST_ID = 2L;

    @Mock
    private MedicalRequestRepository medicalRequestRepository;

    @Mock
    private MedicalPaymentRepository medicalPaymentRepository;

    @Mock
    private MedicalPaymentAttemptRepository attemptRepository;

    @Mock
    private MedicalRequestAdditionalRepository additionalRepository;

    @Mock
    private MedicalRequest request;

    @Mock
    private MedicalRequestProposal proposal;

    @Mock
    private PatientProfile patient;

    @Mock
    private SpecialistProfile specialist;

    @Mock
    private UserAccount patientUser;

    @Mock
    private UserAccount specialistUser;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(
            medicalRequestRepository,
            medicalPaymentRepository,
            additionalRepository,
            attemptRepository
        );

        when(
            medicalRequestRepository.findDetailedByIdForUpdate(
                REQUEST_ID
            )
        ).thenReturn(Optional.of(request));

        when(request.getId()).thenReturn(REQUEST_ID);
        when(request.getRequestCode()).thenReturn("SM-PAY-001");
        when(request.getStatus()).thenReturn("FINALIZADO");
        when(request.getPatientProfile()).thenReturn(patient);
        when(request.getAcceptedSpecialistProfile()).thenReturn(specialist);
        when(request.getAcceptedProposal()).thenReturn(proposal);

        when(patient.getId()).thenReturn(PATIENT_ID);
        when(patient.getUserAccount()).thenReturn(patientUser);

        when(specialist.getId()).thenReturn(SPECIALIST_ID);
        when(specialist.getUserAccount()).thenReturn(specialistUser);

        when(patientUser.getFirstName()).thenReturn("Paciente");
        when(patientUser.getLastName()).thenReturn("Demo");

        when(specialistUser.getFirstName()).thenReturn("Especialista");
        when(specialistUser.getLastName()).thenReturn("Demo");

        when(proposal.getServiceAmount())
            .thenReturn(new BigDecimal("100.00"));

        when(proposal.getMobilityAmount())
            .thenReturn(new BigDecimal("20.00"));
    }

    @Test
    void registerCreatesPendingDirectPaymentWithZeroCommission() {
        when(
            medicalRequestRepository.findDetailedByIdForUpdate(
                REQUEST_ID
            )
        ).thenReturn(Optional.of(request));

        when(
            medicalPaymentRepository.existsByMedicalRequestId(
                REQUEST_ID
            )
        ).thenReturn(false);

        when(
            additionalRepository.existsByMedicalRequestIdAndStatus(
                REQUEST_ID,
                "PENDING"
            )
        ).thenReturn(false);

        when(
            additionalRepository.sumApprovedAmountByRequestId(
                REQUEST_ID
            )
        ).thenReturn(new BigDecimal("30.00"));

        when(
            medicalPaymentRepository.save(any(MedicalPayment.class))
        ).thenAnswer(invocation -> {
            MedicalPayment payment = invocation.getArgument(0);

            ReflectionTestUtils.setField(
                payment,
                "id",
                99L
            );

            return payment;
        });

        MockMultipartFile evidence =
            new MockMultipartFile(
                "evidence",
                "yape.png",
                "image/png",
                new byte[] { 1, 2, 3, 4 }
            );

        ManualPaymentResponse response =
            service.registerForPatient(
                REQUEST_ID,
                PATIENT_ID,
                "YAPE",
                "OP-001",
                evidence
            );

        assertEquals("DIRECT_EXTERNAL", response.paymentFlow());
        assertEquals("PENDING", response.paymentStatus());

        assertEquals(
            new BigDecimal("100.00"),
            response.serviceAmount()
        );

        assertEquals(
            new BigDecimal("20.00"),
            response.mobilityAmount()
        );

        assertEquals(
            new BigDecimal("30.00"),
            response.additionalAmount()
        );

        assertEquals(
            new BigDecimal("150.00"),
            response.totalAmount()
        );

        assertEquals(
            new BigDecimal("0.00"),
            response.platformCommissionAmount()
        );

        assertEquals(
            new BigDecimal("150.00"),
            response.specialistNetAmount()
        );

        assertTrue(response.evidenceAvailable());
        assertNull(response.paidAt());
    }

    @Test
    void confirmCalculatesFivePercentOnlyFromServiceAmount() {
        MedicalPayment payment = new MedicalPayment();

        ReflectionTestUtils.setField(
            payment,
            "id",
            99L
        );

        payment.setMedicalRequest(request);
        payment.setPatientProfile(patient);
        payment.setSpecialistProfile(specialist);

        payment.setServiceAmount(
            new BigDecimal("100.00")
        );

        payment.setMobilityAmount(
            new BigDecimal("20.00")
        );

        payment.setAdditionalAmount(
            new BigDecimal("30.00")
        );

        payment.setAmount(
            new BigDecimal("150.00")
        );

        payment.setCurrency("PEN");
        payment.setPaymentMethod("YAPE");
        payment.setPaymentFlow("DIRECT_EXTERNAL");
        payment.setStatus("PENDING");

        payment.setPlatformCommissionPercent(
            new BigDecimal("5.00")
        );

        payment.setPlatformCommissionAmount(
            new BigDecimal("0.00")
        );

        payment.setSpecialistNetAmount(
            new BigDecimal("150.00")
        );

        payment.setEvidenceFileName("yape.png");
        payment.setEvidenceContentType("image/png");
        payment.setEvidenceSize(4L);
        payment.setEvidenceData(
            new byte[] { 1, 2, 3, 4 }
        );

        when(
            medicalPaymentRepository
                .findDetailedByMedicalRequestIdForUpdate(
                    REQUEST_ID
                )
        ).thenReturn(Optional.of(payment));

        when(
            medicalPaymentRepository.save(
                any(MedicalPayment.class)
            )
        ).thenAnswer(
            invocation -> invocation.getArgument(0)
        );

        ManualPaymentResponse response =
            service.confirmForSpecialist(
                REQUEST_ID,
                SPECIALIST_ID,
                true
            );

        assertEquals("PAID", response.paymentStatus());

        assertEquals(
            new BigDecimal("5.00"),
            response.platformCommissionAmount()
        );

        assertEquals(
            new BigDecimal("145.00"),
            response.specialistNetAmount()
        );

        assertTrue(
            response.verificationWarningAcknowledged()
        );

        assertTrue(response.paidAt() != null);
        assertTrue(response.verifiedAt() != null);
    }

    @Test
    void confirmRequiresWarningAcknowledgement() {
        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () ->
                    service.confirmForSpecialist(
                        REQUEST_ID,
                        SPECIALIST_ID,
                        false
                    )
            );

        assertEquals(
            400,
            exception.getStatusCode().value()
        );
    }

    @Test
    void rejectArchivesAttemptAndMarksPaymentRejected() {
        MedicalPayment payment =
            createPayment("PENDING");

        AtomicReference<MedicalPaymentAttempt> archived =
            new AtomicReference<>();

        when(
            medicalPaymentRepository
                .findDetailedByMedicalRequestIdForUpdate(
                    REQUEST_ID
                )
        ).thenReturn(Optional.of(payment));

        when(
            attemptRepository.countByMedicalPaymentId(
                99L
            )
        ).thenReturn(0L);

        when(
            attemptRepository.save(
                any(MedicalPaymentAttempt.class)
            )
        ).thenAnswer(invocation -> {
            MedicalPaymentAttempt attempt =
                invocation.getArgument(0);

            archived.set(attempt);

            return attempt;
        });

        when(
            medicalPaymentRepository.save(
                any(MedicalPayment.class)
            )
        ).thenAnswer(
            invocation -> invocation.getArgument(0)
        );

        ManualPaymentResponse response =
            service.rejectForSpecialist(
                REQUEST_ID,
                SPECIALIST_ID,
                "El comprobante no corresponde al abono recibido."
            );

        assertEquals(
            "REJECTED",
            response.paymentStatus()
        );

        assertEquals(
            "El comprobante no corresponde al abono recibido.",
            response.rejectionReason()
        );

        assertTrue(
            response.rejectedAt() != null
        );

        assertNull(
            response.paidAt()
        );

        assertNull(
            response.verifiedAt()
        );

        assertEquals(
            new BigDecimal("0.00"),
            response.platformCommissionAmount()
        );

        assertEquals(
            new BigDecimal("150.00"),
            response.specialistNetAmount()
        );

        MedicalPaymentAttempt attempt =
            archived.get();

        assertTrue(
            attempt != null
        );

        assertEquals(
            1,
            attempt.getAttemptNumber()
        );

        assertEquals(
            "YAPE",
            attempt.getPaymentMethod()
        );

        assertEquals(
            "OP-OLD",
            attempt.getExternalTransactionId()
        );

        assertEquals(
            "pago-original.png",
            attempt.getEvidenceFileName()
        );

        assertEquals(
            "El comprobante no corresponde al abono recibido.",
            attempt.getRejectionReason()
        );

        assertEquals(
            SPECIALIST_ID,
            attempt
                .getRejectedBySpecialistProfile()
                .getId()
        );
    }

    @Test
    void resubmitRejectedPaymentCreatesNewPendingAttempt() {
        MedicalPayment payment =
            createPayment("REJECTED");

        payment.setRejectedAt(
            OffsetDateTime.parse(
                "2026-08-15T17:00:00-05:00"
            )
        );

        payment.setRejectedBySpecialistProfile(
            specialist
        );

        payment.setRejectionReason(
            "Comprobante anterior rechazado."
        );

        when(
            medicalPaymentRepository
                .findDetailedByMedicalRequestIdForUpdate(
                    REQUEST_ID
                )
        ).thenReturn(Optional.of(payment));

        when(
            additionalRepository.existsByMedicalRequestIdAndStatus(
                REQUEST_ID,
                "PENDING"
            )
        ).thenReturn(false);

        when(
            medicalPaymentRepository.save(
                any(MedicalPayment.class)
            )
        ).thenAnswer(
            invocation -> invocation.getArgument(0)
        );

        MockMultipartFile newEvidence =
            new MockMultipartFile(
                "evidence",
                "pago-corregido.png",
                "image/png",
                new byte[] { 9, 8, 7, 6 }
            );

        ManualPaymentResponse response =
            service.registerForPatient(
                REQUEST_ID,
                PATIENT_ID,
                "PLIN",
                "OP-NEW",
                newEvidence
            );

        assertEquals(
            "PENDING",
            response.paymentStatus()
        );

        assertEquals(
            "PLIN",
            response.paymentMethod()
        );

        assertEquals(
            "OP-NEW",
            response.externalTransactionId()
        );

        assertEquals(
            "pago-corregido.png",
            response.evidenceFileName()
        );

        assertNull(
            response.rejectedAt()
        );

        assertNull(
            response.rejectionReason()
        );

        assertNull(
            response.paidAt()
        );

        assertNull(
            response.verifiedAt()
        );

        assertEquals(
            new BigDecimal("0.00"),
            response.platformCommissionAmount()
        );

        assertEquals(
            new BigDecimal("150.00"),
            response.specialistNetAmount()
        );
    }

    private MedicalPayment createPayment(
        String status
    ) {
        MedicalPayment payment =
            new MedicalPayment();

        ReflectionTestUtils.setField(
            payment,
            "id",
            99L
        );

        payment.setMedicalRequest(request);
        payment.setPatientProfile(patient);
        payment.setSpecialistProfile(specialist);

        payment.setServiceAmount(
            new BigDecimal("100.00")
        );

        payment.setMobilityAmount(
            new BigDecimal("20.00")
        );

        payment.setAdditionalAmount(
            new BigDecimal("30.00")
        );

        payment.setAmount(
            new BigDecimal("150.00")
        );

        payment.setCurrency("PEN");
        payment.setPaymentMethod("YAPE");
        payment.setPaymentFlow("DIRECT_EXTERNAL");
        payment.setStatus(status);

        payment.setExternalTransactionId(
            "OP-OLD"
        );

        payment.setPlatformCommissionPercent(
            new BigDecimal("5.00")
        );

        payment.setPlatformCommissionAmount(
            new BigDecimal("0.00")
        );

        payment.setSpecialistNetAmount(
            new BigDecimal("150.00")
        );

        payment.setEvidenceFileName(
            "pago-original.png"
        );

        payment.setEvidenceContentType(
            "image/png"
        );

        payment.setEvidenceSize(
            4L
        );

        payment.setEvidenceData(
            new byte[] { 1, 2, 3, 4 }
        );

        payment.setEvidenceUploadedAt(
            OffsetDateTime.parse(
                "2026-08-15T16:30:00-05:00"
            )
        );

        return payment;
    }
}
