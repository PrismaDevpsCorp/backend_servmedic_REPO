package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectManualPaymentRequest(
    @NotBlank(message = "El motivo del rechazo es obligatorio.")
    @Size(
        max = 500,
        message = "El motivo del rechazo no puede superar 500 caracteres."
    )
    String reason
) {
}
