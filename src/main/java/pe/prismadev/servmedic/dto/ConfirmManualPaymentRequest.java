package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.AssertTrue;

public record ConfirmManualPaymentRequest(
    @AssertTrue(
        message = "Debe confirmar que verifico correctamente el pago antes de continuar."
    )
    boolean warningAcknowledged
) {
}
