package pe.prismadev.servmedic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CreateMedicalRequestAdditionalRequest;
import pe.prismadev.servmedic.dto.MedicalRequestAdditionalResponse;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.MedicalRequestAdditional;
import pe.prismadev.servmedic.entity.MedicalRequestProposal;
import pe.prismadev.servmedic.entity.PatientProfile;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalRequestAdditionalRepository;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicalRequestAdditionalServiceTest {

    private static final Long REQUEST_ID = 100L;
    private static final Long PATIENT_ID = 10L;
    private static final Long SPECIALIST_ID = 20L;
    private static final Long ADDITIONAL_ID = 700L;

    @Mock
    private MedicalRequestRepository medicalRequestRepository;

    @Mock
    private MedicalRequestAdditionalRepository additionalRepository;

    @Mock
    private PatientProfile patientProfile;

    @Mock
    private SpecialistProfile specialistProfile;

    @Mock
    private UserAccount specialistUser;

    @Mock
    private MedicalRequestProposal acceptedProposal;

    private MedicalRequestAdditionalService service;
    private MedicalRequest medicalRequest;

    @BeforeEach
    void setUp() {
        service = new MedicalRequestAdditionalService(
            medicalRequestRepository,
            additionalRepository
        );

        medicalRequest = new MedicalRequest();

        ReflectionTestUtils.setField(
            medicalRequest,
            "id",
            REQUEST_ID
        );

        ReflectionTestUtils.setField(
            medicalRequest,
            "status",
            "EN_ATENCION"
        );

        ReflectionTestUtils.setField(
            medicalRequest,
            "acceptedSpecialistProfile",
            specialistProfile
        );

        ReflectionTestUtils.setField(
            medicalRequest,
            "acceptedProposal",
            acceptedProposal
        );

        medicalRequest.setRequestCode("SM-ADD-001");
        medicalRequest.setPatientProfile(patientProfile);

        when(patientProfile.getId()).thenReturn(PATIENT_ID);
        when(specialistProfile.getId())
            .thenReturn(SPECIALIST_ID);
        when(specialistProfile.getUserAccount())
            .thenReturn(specialistUser);
        when(specialistUser.getFirstName())
            .thenReturn("Medico");
        when(specialistUser.getLastName())
            .thenReturn("Demo");
        when(acceptedProposal.getTotalAmount())
            .thenReturn(new BigDecimal("105.00"));
    }

    @Test
    void createForSpecialistPersistsPendingAdditional() {
        configureRequestForUpdate();

        when(additionalRepository.saveAndFlush(any()))
            .thenAnswer(invocation -> {
                MedicalRequestAdditional additional =
                    invocation.getArgument(0);

                ReflectionTestUtils.setField(
                    additional,
                    "id",
                    ADDITIONAL_ID
                );

                additional.beforeInsert();
                return additional;
            });

        when(
            additionalRepository.sumApprovedAmountByRequestId(
                REQUEST_ID
            )
        ).thenReturn(BigDecimal.ZERO);

        CreateMedicalRequestAdditionalRequest request =
            new CreateMedicalRequestAdditionalRequest(
                "  Material de curacion  ",
                "  Se requiere material esteril adicional.  ",
                new BigDecimal("25.00")
            );

        MedicalRequestAdditionalResponse response =
            service.createForSpecialist(
                REQUEST_ID,
                SPECIALIST_ID,
                request
            );

        ArgumentCaptor<MedicalRequestAdditional> captor =
            ArgumentCaptor.forClass(
                MedicalRequestAdditional.class
            );

        verify(additionalRepository).saveAndFlush(
            captor.capture()
        );

        MedicalRequestAdditional saved = captor.getValue();

        assertSame(
            medicalRequest,
            saved.getMedicalRequest()
        );
        assertSame(
            specialistProfile,
            saved.getSpecialistProfile()
        );
        assertEquals(
            "Material de curacion",
            saved.getConcept()
        );
        assertEquals(
            "Se requiere material esteril adicional.",
            saved.getJustification()
        );
        assertEquals(
            new BigDecimal("25.00"),
            saved.getAmount()
        );
        assertEquals("PENDING", response.status());
        assertEquals(
            new BigDecimal("105.00"),
            response.originalTotalAmount()
        );
        assertEquals(
            new BigDecimal("0.00"),
            response.approvedAdditionalsAmount()
        );
        assertEquals(
            new BigDecimal("105.00"),
            response.currentTotalAmount()
        );
    }

    @Test
    void approveForPatientAddsApprovedAmountToCurrentTotal() {
        configureRequestForUpdate();

        MedicalRequestAdditional additional =
            newAdditional(ADDITIONAL_ID);

        when(
            additionalRepository
                .findDetailedByIdAndRequestIdForUpdate(
                    ADDITIONAL_ID,
                    REQUEST_ID
                )
        ).thenReturn(Optional.of(additional));

        when(
            additionalRepository.sumApprovedAmountByRequestId(
                REQUEST_ID
            )
        ).thenReturn(new BigDecimal("25.00"));

        MedicalRequestAdditionalResponse response =
            service.approveForPatient(
                REQUEST_ID,
                ADDITIONAL_ID,
                PATIENT_ID
            );

        assertEquals("APPROVED", additional.getStatus());
        assertTrue(additional.getRespondedAt() != null);
        assertEquals("APPROVED", response.status());
        assertEquals(
            new BigDecimal("25.00"),
            response.approvedAdditionalsAmount()
        );
        assertEquals(
            new BigDecimal("130.00"),
            response.currentTotalAmount()
        );
        verify(additionalRepository).flush();
    }

    @Test
    void rejectForPatientKeepsOriginalTotal() {
        configureRequestForUpdate();

        MedicalRequestAdditional additional =
            newAdditional(ADDITIONAL_ID);

        when(
            additionalRepository
                .findDetailedByIdAndRequestIdForUpdate(
                    ADDITIONAL_ID,
                    REQUEST_ID
                )
        ).thenReturn(Optional.of(additional));

        when(
            additionalRepository.sumApprovedAmountByRequestId(
                REQUEST_ID
            )
        ).thenReturn(BigDecimal.ZERO);

        MedicalRequestAdditionalResponse response =
            service.rejectForPatient(
                REQUEST_ID,
                ADDITIONAL_ID,
                PATIENT_ID
            );

        assertEquals("REJECTED", additional.getStatus());
        assertEquals(
            new BigDecimal("0.00"),
            response.approvedAdditionalsAmount()
        );
        assertEquals(
            new BigDecimal("105.00"),
            response.currentTotalAmount()
        );
    }

    @Test
    void createForSpecialistRejectsDifferentSpecialist() {
        configureRequestForUpdate();

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createForSpecialist(
                REQUEST_ID,
                99L,
                validCreateRequest()
            )
        );

        assertEquals(
            HttpStatus.FORBIDDEN,
            exception.getStatusCode()
        );
        verify(additionalRepository, never())
            .saveAndFlush(any());
    }

    @Test
    void createForSpecialistRequiresAttentionStatus() {
        configureRequestForUpdate();

        ReflectionTestUtils.setField(
            medicalRequest,
            "status",
            "EN_CAMINO"
        );

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createForSpecialist(
                REQUEST_ID,
                SPECIALIST_ID,
                validCreateRequest()
            )
        );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatusCode()
        );
        assertTrue(
            exception.getReason().contains("EN_ATENCION")
        );
        verify(additionalRepository, never())
            .saveAndFlush(any());
    }

    @Test
    void patientCannotResolveAnotherPatientsAdditional() {
        configureRequestForUpdate();

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.approveForPatient(
                REQUEST_ID,
                ADDITIONAL_ID,
                999L
            )
        );

        assertEquals(
            HttpStatus.FORBIDDEN,
            exception.getStatusCode()
        );
        verify(
            additionalRepository,
            never()
        ).findDetailedByIdAndRequestIdForUpdate(
            any(),
            any()
        );
    }

    @Test
    void listForPatientReturnsTraceabilityAndTotals() {
        when(
            medicalRequestRepository.findDetailedById(
                REQUEST_ID
            )
        ).thenReturn(Optional.of(medicalRequest));

        MedicalRequestAdditional approved =
            newAdditional(701L);
        approved.approve(
            java.time.OffsetDateTime.now()
        );

        MedicalRequestAdditional rejected =
            newAdditional(702L);
        rejected.reject(
            java.time.OffsetDateTime.now()
        );

        when(
            additionalRepository.findDetailedByRequestId(
                REQUEST_ID
            )
        ).thenReturn(List.of(
            approved,
            rejected
        ));

        when(
            additionalRepository.sumApprovedAmountByRequestId(
                REQUEST_ID
            )
        ).thenReturn(new BigDecimal("25.00"));

        List<MedicalRequestAdditionalResponse> responses =
            service.listForPatient(
                REQUEST_ID,
                PATIENT_ID
            );

        assertEquals(2, responses.size());
        assertEquals(
            new BigDecimal("130.00"),
            responses.get(0).currentTotalAmount()
        );
        assertEquals(
            new BigDecimal("130.00"),
            responses.get(1).currentTotalAmount()
        );
    }

    private void configureRequestForUpdate() {
        when(
            medicalRequestRepository.findDetailedByIdForUpdate(
                REQUEST_ID
            )
        ).thenReturn(Optional.of(medicalRequest));
    }

    private CreateMedicalRequestAdditionalRequest
        validCreateRequest() {
        return new CreateMedicalRequestAdditionalRequest(
            "Material de curacion",
            "Se requiere material esteril adicional.",
            new BigDecimal("25.00")
        );
    }

    private MedicalRequestAdditional newAdditional(
        Long additionalId
    ) {
        MedicalRequestAdditional additional =
            new MedicalRequestAdditional(
                medicalRequest,
                specialistProfile,
                "Material de curacion",
                "Se requiere material esteril adicional.",
                new BigDecimal("25.00")
            );

        ReflectionTestUtils.setField(
            additional,
            "id",
            additionalId
        );

        additional.beforeInsert();
        return additional;
    }
}
