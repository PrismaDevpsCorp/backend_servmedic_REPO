package pe.prismadev.servmedic.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MedicalRequestProposalTest {

    private static final OffsetDateTime NOW =
        OffsetDateTime.parse("2026-07-31T21:30:00-05:00");

    private MedicalRequest medicalRequest;
    private SpecialistProfile specialistProfile;

    @BeforeEach
    void setUp() {
        medicalRequest = new MedicalRequest();
        specialistProfile = mock(SpecialistProfile.class);
    }

    @Test
    void newProposalStartsPendingAndUsesExactExpirationBoundary() {
        OffsetDateTime expiresAt = NOW.plusMinutes(30);
        MedicalRequestProposal proposal = createProposal(
            medicalRequest,
            expiresAt
        );

        assertTrue(proposal.isPending());
        assertEquals("PENDING", proposal.getStatus());
        assertEquals("PEN", proposal.getCurrency());
        assertFalse(proposal.isExpiredAt(expiresAt.minusNanos(1)));
        assertTrue(proposal.isExpiredAt(expiresAt));
        assertTrue(proposal.isExpiredAt(expiresAt.plusSeconds(1)));
    }

    @Test
    void acceptChangesPendingProposalToAccepted() {
        MedicalRequestProposal proposal = createProposal(
            medicalRequest,
            NOW.plusMinutes(30)
        );

        proposal.accept(NOW);

        assertEquals("ACCEPTED", proposal.getStatus());
        assertEquals(NOW, proposal.getRespondedAt());
        assertNull(proposal.getWithdrawnAt());
    }

    @Test
    void rejectChangesPendingProposalToRejected() {
        MedicalRequestProposal proposal = createProposal(
            medicalRequest,
            NOW.plusMinutes(30)
        );

        proposal.reject(NOW);

        assertEquals("REJECTED", proposal.getStatus());
        assertEquals(NOW, proposal.getRespondedAt());
        assertNull(proposal.getWithdrawnAt());
    }

    @Test
    void withdrawChangesPendingProposalToWithdrawn() {
        MedicalRequestProposal proposal = createProposal(
            medicalRequest,
            NOW.plusMinutes(30)
        );

        proposal.withdraw(NOW);

        assertEquals("WITHDRAWN", proposal.getStatus());
        assertEquals(NOW, proposal.getWithdrawnAt());
        assertNull(proposal.getRespondedAt());
    }

    @Test
    void expireChangesPendingProposalToExpired() {
        MedicalRequestProposal proposal = createProposal(
            medicalRequest,
            NOW
        );

        proposal.expire(NOW);

        assertEquals("EXPIRED", proposal.getStatus());
        assertEquals(NOW, proposal.getRespondedAt());
        assertNull(proposal.getWithdrawnAt());
    }

    @Test
    void completedProposalCannotChangeStateAgain() {
        MedicalRequestProposal proposal = createProposal(
            medicalRequest,
            NOW.plusMinutes(30)
        );

        proposal.accept(NOW);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> proposal.reject(NOW.plusMinutes(1))
        );

        assertTrue(exception.getMessage().contains("ACCEPTED"));
        assertEquals("ACCEPTED", proposal.getStatus());
        assertEquals(NOW, proposal.getRespondedAt());
    }

    @Test
    void medicalRequestAcceptsProposalAndUpdatesCommercialSnapshot() {
        MedicalRequestProposal proposal = createProposal(
            medicalRequest,
            NOW.plusMinutes(30)
        );

        medicalRequest.acceptProposal(proposal, NOW);

        assertEquals("ACCEPTED", medicalRequest.getStatus());
        assertSame(proposal, medicalRequest.getAcceptedProposal());
        assertSame(
            specialistProfile,
            medicalRequest.getAcceptedSpecialistProfile()
        );
        assertEquals(
            new BigDecimal("110.00"),
            medicalRequest.getEstimatedAmount()
        );
        assertEquals(NOW, medicalRequest.getAcceptedAt());
        assertEquals("ACCEPTED", proposal.getStatus());
        assertEquals(NOW, proposal.getRespondedAt());
    }

    @Test
    void medicalRequestRejectsExpiredProposal() {
        MedicalRequestProposal proposal = createProposal(
            medicalRequest,
            NOW
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> medicalRequest.acceptProposal(proposal, NOW)
        );

        assertTrue(exception.getMessage().contains("expiro"));
        assertEquals("PENDING", medicalRequest.getStatus());
        assertNull(medicalRequest.getAcceptedProposal());
        assertTrue(proposal.isPending());
    }

    @Test
    void medicalRequestRejectsProposalFromAnotherRequest() {
        MedicalRequest anotherRequest = new MedicalRequest();
        MedicalRequestProposal foreignProposal = createProposal(
            anotherRequest,
            NOW.plusMinutes(30)
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> medicalRequest.acceptProposal(foreignProposal, NOW)
        );

        assertTrue(exception.getMessage().contains("no pertenece"));
        assertEquals("PENDING", medicalRequest.getStatus());
        assertNull(medicalRequest.getAcceptedProposal());
        assertTrue(foreignProposal.isPending());
    }

    private MedicalRequestProposal createProposal(
        MedicalRequest request,
        OffsetDateTime expiresAt
    ) {
        MedicalRequestProposal proposal = new MedicalRequestProposal(
            request,
            specialistProfile,
            new BigDecimal("95.00"),
            "SEPARATE",
            new BigDecimal("15.00"),
            new BigDecimal("110.00"),
            "Atencion medica domiciliaria",
            25,
            expiresAt
        );

        proposal.beforeInsert();
        return proposal;
    }
}
