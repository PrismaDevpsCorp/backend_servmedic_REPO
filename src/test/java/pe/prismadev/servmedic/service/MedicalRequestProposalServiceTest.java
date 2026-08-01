package pe.prismadev.servmedic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CreateMedicalRequestProposalRequest;
import pe.prismadev.servmedic.dto.MedicalRequestProposalResponse;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.MedicalRequestProposal;
import pe.prismadev.servmedic.entity.MedicalService;
import pe.prismadev.servmedic.entity.PatientProfile;
import pe.prismadev.servmedic.entity.Profession;
import pe.prismadev.servmedic.entity.SpecialistCommercialProfile;
import pe.prismadev.servmedic.entity.SpecialistOfferedService;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalRequestProposalRepository;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;
import pe.prismadev.servmedic.repository.SpecialistCommercialProfileRepository;
import pe.prismadev.servmedic.repository.SpecialistOfferedServiceRepository;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicalRequestProposalServiceTest {

    private static final Long REQUEST_ID = 100L;
    private static final Long PATIENT_ID = 10L;
    private static final Long SPECIALIST_ID = 20L;
    private static final Long PROPOSAL_ID = 501L;

    @Mock
    private MedicalRequestRepository medicalRequestRepository;

    @Mock
    private MedicalRequestProposalRepository proposalRepository;

    @Mock
    private SpecialistProfileRepository specialistProfileRepository;

    @Mock
    private SpecialistOfferedServiceRepository offeredServiceRepository;

    @Mock
    private SpecialistCommercialProfileRepository commercialProfileRepository;

    @Mock
    private PatientProfile patientProfile;

    @Mock
    private MedicalService medicalService;

    @Mock
    private Profession profession;

    @Mock
    private SpecialistProfile specialist;

    @Mock
    private SpecialistProfile otherSpecialist;

    @Mock
    private UserAccount specialistUser;

    @Mock
    private SpecialistOfferedService offeredService;

    @Mock
    private SpecialistCommercialProfile commercialProfile;

    private MedicalRequestProposalService service;
    private MedicalRequest medicalRequest;

    @BeforeEach
    void setUp() {
        service = new MedicalRequestProposalService(
            medicalRequestRepository,
            proposalRepository,
            specialistProfileRepository,
            offeredServiceRepository,
            commercialProfileRepository
        );

        medicalRequest = new MedicalRequest();
        ReflectionTestUtils.setField(
            medicalRequest,
            "id",
            REQUEST_ID
        );
        medicalRequest.setRequestCode("SM-TEST-001");
        medicalRequest.setPatientProfile(patientProfile);
        medicalRequest.setMedicalService(medicalService);

        when(patientProfile.getId()).thenReturn(PATIENT_ID);

        when(medicalService.getId()).thenReturn(6L);
        when(medicalService.getCode())
            .thenReturn("CONSULTA_MEDICA_GENERAL");
        when(medicalService.getName())
            .thenReturn("Consulta medica general");
        when(medicalService.getProfession())
            .thenReturn(profession);

        when(profession.getCode()).thenReturn("MEDICO_GENERAL");
        when(profession.getName()).thenReturn("Medico general");

        when(specialist.getId()).thenReturn(SPECIALIST_ID);
        when(specialist.getStatus()).thenReturn("ACTIVE");
        when(specialist.isAvailable()).thenReturn(true);
        when(specialist.getProfession()).thenReturn(profession);
        when(specialist.getUserAccount()).thenReturn(specialistUser);

        when(specialistUser.getFirstName()).thenReturn("Maria");
        when(specialistUser.getLastName()).thenReturn("Demo");
    }

    @Test
    void createProposalBuildsCommercialSnapshotAndPersistsIt() {
        configureSuccessfulCreation();

        when(proposalRepository.saveAndFlush(any()))
            .thenAnswer(invocation -> {
                MedicalRequestProposal proposal =
                    invocation.getArgument(0);
                ReflectionTestUtils.setField(
                    proposal,
                    "id",
                    PROPOSAL_ID
                );
                proposal.beforeInsert();
                return proposal;
            });

        CreateMedicalRequestProposalRequest request =
            new CreateMedicalRequestProposalRequest(
                25,
                30,
                "  Atencion domiciliaria inmediata  "
            );

        MedicalRequestProposalResponse response =
            service.createProposal(
                REQUEST_ID,
                SPECIALIST_ID,
                request
            );

        ArgumentCaptor<MedicalRequestProposal> captor =
            ArgumentCaptor.forClass(
                MedicalRequestProposal.class
            );

        verify(proposalRepository).saveAndFlush(
            captor.capture()
        );

        MedicalRequestProposal saved = captor.getValue();

        assertSame(medicalRequest, saved.getMedicalRequest());
        assertSame(specialist, saved.getSpecialistProfile());
        assertEquals(
            new BigDecimal("95.00"),
            saved.getServiceAmount()
        );
        assertEquals(
            new BigDecimal("15.00"),
            saved.getMobilityAmount()
        );
        assertEquals(
            new BigDecimal("110.00"),
            saved.getTotalAmount()
        );
        assertEquals("SEPARATE", saved.getMobilityPolicySnapshot());
        assertEquals(
            "Atencion domiciliaria inmediata",
            saved.getProposalMessage()
        );
        assertEquals(25, saved.getEstimatedArrivalMinutes());
        assertEquals("PENDING", response.status());
        assertEquals(PROPOSAL_ID, response.proposalId());
        assertEquals(
            new BigDecimal("110.00"),
            response.totalAmount()
        );
        assertEquals("PEN", response.currency());
    }

    @Test
    void createProposalRejectsAnExistingUnexpiredPendingProposal() {
        configureRequestAndSpecialist();

        MedicalRequestProposal existing =
            newProposal(
                specialist,
                OffsetDateTime.now().plusHours(1),
                601L
            );

        when(
            proposalRepository
                .findPendingByRequestAndSpecialistForUpdate(
                    REQUEST_ID,
                    SPECIALIST_ID
                )
        ).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createProposal(
                REQUEST_ID,
                SPECIALIST_ID,
                new CreateMedicalRequestProposalRequest(
                    25,
                    30,
                    null
                )
            )
        );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatusCode()
        );
        assertTrue(
            exception.getReason().contains(
                "ya tiene una propuesta pendiente"
            )
        );
        verify(proposalRepository, never())
            .saveAndFlush(any());
    }

    @Test
    void createProposalExpiresStalePendingBeforeInsertingReplacement() {
        configureSuccessfulCreation();

        MedicalRequestProposal stale =
            newProposal(
                specialist,
                OffsetDateTime.now().minusHours(1),
                602L
            );

        when(
            proposalRepository
                .findPendingByRequestAndSpecialistForUpdate(
                    REQUEST_ID,
                    SPECIALIST_ID
                )
        ).thenReturn(Optional.of(stale));

        when(proposalRepository.saveAndFlush(any()))
            .thenAnswer(invocation -> {
                MedicalRequestProposal proposal =
                    invocation.getArgument(0);
                ReflectionTestUtils.setField(
                    proposal,
                    "id",
                    PROPOSAL_ID
                );
                proposal.beforeInsert();
                return proposal;
            });

        service.createProposal(
            REQUEST_ID,
            SPECIALIST_ID,
            new CreateMedicalRequestProposalRequest(
                30,
                null,
                null
            )
        );

        assertEquals("EXPIRED", stale.getStatus());

        InOrder order = inOrder(proposalRepository);
        order.verify(proposalRepository).flush();
        order.verify(proposalRepository)
            .saveAndFlush(any());
    }

    @Test
    void acceptForPatientAcceptsSelectedAndRejectsRemaining() {
        MedicalRequestProposal selected =
            newProposal(
                specialist,
                OffsetDateTime.now().plusHours(1),
                PROPOSAL_ID
            );

        MedicalRequestProposal other =
            newProposal(
                otherSpecialist,
                OffsetDateTime.now().plusHours(1),
                502L
            );

        when(
            medicalRequestRepository.findDetailedByIdForUpdate(
                REQUEST_ID
            )
        ).thenReturn(Optional.of(medicalRequest));

        when(
            proposalRepository
                .findDetailedByIdAndRequestIdForUpdate(
                    PROPOSAL_ID,
                    REQUEST_ID
                )
        ).thenReturn(Optional.of(selected));

        when(
            proposalRepository.findPendingByRequestIdForUpdate(
                REQUEST_ID
            )
        ).thenReturn(List.of(selected, other));

        MedicalRequestProposalResponse response =
            service.acceptForPatient(
                REQUEST_ID,
                PROPOSAL_ID,
                PATIENT_ID
            );

        assertEquals("ACCEPTED", medicalRequest.getStatus());
        assertSame(
            selected,
            medicalRequest.getAcceptedProposal()
        );
        assertSame(
            specialist,
            medicalRequest.getAcceptedSpecialistProfile()
        );
        assertEquals(
            new BigDecimal("110.00"),
            medicalRequest.getEstimatedAmount()
        );
        assertEquals("ACCEPTED", selected.getStatus());
        assertEquals("REJECTED", other.getStatus());
        assertEquals("ACCEPTED", response.status());

        verify(proposalRepository).flush();
    }

    @Test
    void acceptForPatientRejectsExpiredProposalWithoutMutatingIt() {
        MedicalRequestProposal expired =
            newProposal(
                specialist,
                OffsetDateTime.now().minusHours(1),
                PROPOSAL_ID
            );

        when(
            medicalRequestRepository.findDetailedByIdForUpdate(
                REQUEST_ID
            )
        ).thenReturn(Optional.of(medicalRequest));

        when(
            proposalRepository
                .findDetailedByIdAndRequestIdForUpdate(
                    PROPOSAL_ID,
                    REQUEST_ID
                )
        ).thenReturn(Optional.of(expired));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.acceptForPatient(
                REQUEST_ID,
                PROPOSAL_ID,
                PATIENT_ID
            )
        );

        assertEquals(
            HttpStatus.GONE,
            exception.getStatusCode()
        );
        assertEquals("PENDING", medicalRequest.getStatus());
        assertEquals("PENDING", expired.getStatus());
        assertNull(medicalRequest.getAcceptedProposal());
        verify(proposalRepository, never()).flush();
    }

    @Test
    void withdrawForSpecialistMarksCurrentProposalAsWithdrawn() {
        MedicalRequestProposal proposal =
            newProposal(
                specialist,
                OffsetDateTime.now().plusHours(1),
                PROPOSAL_ID
            );

        when(
            medicalRequestRepository.findDetailedByIdForUpdate(
                REQUEST_ID
            )
        ).thenReturn(Optional.of(medicalRequest));

        when(
            proposalRepository
                .findDetailedByIdAndRequestIdForUpdate(
                    PROPOSAL_ID,
                    REQUEST_ID
                )
        ).thenReturn(Optional.of(proposal));

        MedicalRequestProposalResponse response =
            service.withdrawForSpecialist(
                REQUEST_ID,
                PROPOSAL_ID,
                SPECIALIST_ID
            );

        assertEquals("WITHDRAWN", proposal.getStatus());
        assertTrue(proposal.getWithdrawnAt() != null);
        assertEquals("WITHDRAWN", response.status());
        verify(proposalRepository).flush();
    }

    @Test
    void listForPatientExpiresOnlyStalePendingProposals() {
        MedicalRequestProposal stale =
            newProposal(
                specialist,
                OffsetDateTime.now().minusHours(1),
                701L
            );

        MedicalRequestProposal current =
            newProposal(
                specialist,
                OffsetDateTime.now().plusHours(1),
                702L
            );

        when(
            medicalRequestRepository.findDetailedByIdForUpdate(
                REQUEST_ID
            )
        ).thenReturn(Optional.of(medicalRequest));

        when(
            proposalRepository.findDetailedByRequestId(
                REQUEST_ID
            )
        ).thenReturn(List.of(stale, current));

        List<MedicalRequestProposalResponse> responses =
            service.listForPatient(
                REQUEST_ID,
                PATIENT_ID
            );

        assertEquals(2, responses.size());
        assertEquals("EXPIRED", stale.getStatus());
        assertEquals("PENDING", current.getStatus());
        verify(proposalRepository).flush();
    }

    private void configureRequestAndSpecialist() {
        when(
            medicalRequestRepository.findDetailedByIdForUpdate(
                REQUEST_ID
            )
        ).thenReturn(Optional.of(medicalRequest));

        when(
            specialistProfileRepository.findDetailedById(
                SPECIALIST_ID
            )
        ).thenReturn(Optional.of(specialist));

        when(
            proposalRepository
                .findPendingByRequestAndSpecialistForUpdate(
                    REQUEST_ID,
                    SPECIALIST_ID
                )
        ).thenReturn(Optional.empty());
    }

    private void configureSuccessfulCreation() {
        configureRequestAndSpecialist();

        when(
            offeredServiceRepository
                .findActiveDetailedBySpecialistProfileIdAndMedicalServiceId(
                    SPECIALIST_ID,
                    6L
                )
        ).thenReturn(Optional.of(offeredService));

        when(offeredService.getBasePrice())
            .thenReturn(new BigDecimal("95.00"));

        when(
            commercialProfileRepository
                .findDetailedBySpecialistProfileId(
                    SPECIALIST_ID
                )
        ).thenReturn(Optional.of(commercialProfile));

        when(commercialProfile.isActive()).thenReturn(true);
        when(commercialProfile.getMobilityPolicy())
            .thenReturn("SEPARATE");
        when(commercialProfile.getMobilityReferenceAmount())
            .thenReturn(new BigDecimal("15.00"));
    }

    private MedicalRequestProposal newProposal(
        SpecialistProfile proposalSpecialist,
        OffsetDateTime expiresAt,
        Long proposalId
    ) {
        MedicalRequestProposal proposal =
            new MedicalRequestProposal(
                medicalRequest,
                proposalSpecialist,
                new BigDecimal("95.00"),
                "SEPARATE",
                new BigDecimal("15.00"),
                new BigDecimal("110.00"),
                "Atencion domiciliaria",
                25,
                expiresAt
            );

        ReflectionTestUtils.setField(
            proposal,
            "id",
            proposalId
        );
        proposal.beforeInsert();
        return proposal;
    }
}
