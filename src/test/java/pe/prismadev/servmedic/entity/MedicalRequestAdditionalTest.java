package pe.prismadev.servmedic.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MedicalRequestAdditionalTest {

    private static final OffsetDateTime NOW =
        OffsetDateTime.parse("2026-08-04T18:45:00-05:00");

    private MedicalRequestAdditional additional;

    @BeforeEach
    void setUp() {
        MedicalRequest medicalRequest =
            new MedicalRequest();

        SpecialistProfile specialist =
            mock(SpecialistProfile.class);

        additional = new MedicalRequestAdditional(
            medicalRequest,
            specialist,
            "Material de curacion",
            "Se requiere material esteril adicional.",
            new BigDecimal("25.00")
        );

        additional.beforeInsert();
    }

    @Test
    void newAdditionalStartsPending() {
        assertEquals("PENDING", additional.getStatus());
        assertEquals("PEN", additional.getCurrency());
        assertEquals(
            new BigDecimal("25.00"),
            additional.getAmount()
        );
        assertTrue(additional.isPending());
        assertNull(additional.getRespondedAt());
        assertNull(additional.getWithdrawnAt());
    }

    @Test
    void approveChangesPendingAdditionalToApproved() {
        additional.approve(NOW);

        assertEquals("APPROVED", additional.getStatus());
        assertEquals(NOW, additional.getRespondedAt());
        assertNull(additional.getWithdrawnAt());
    }

    @Test
    void rejectChangesPendingAdditionalToRejected() {
        additional.reject(NOW);

        assertEquals("REJECTED", additional.getStatus());
        assertEquals(NOW, additional.getRespondedAt());
        assertNull(additional.getWithdrawnAt());
    }

    @Test
    void withdrawChangesPendingAdditionalToWithdrawn() {
        additional.withdraw(NOW);

        assertEquals("WITHDRAWN", additional.getStatus());
        assertEquals(NOW, additional.getWithdrawnAt());
        assertNull(additional.getRespondedAt());
    }

    @Test
    void resolvedAdditionalCannotChangeStateAgain() {
        additional.approve(NOW);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> additional.reject(NOW.plusMinutes(1))
        );

        assertTrue(
            exception.getMessage().contains("APPROVED")
        );
        assertEquals("APPROVED", additional.getStatus());
    }

    @Test
    void transitionRequiresDecisionTime() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> additional.approve(null)
        );

        assertTrue(
            exception.getMessage().contains("fecha")
        );
        assertEquals("PENDING", additional.getStatus());
    }
}
