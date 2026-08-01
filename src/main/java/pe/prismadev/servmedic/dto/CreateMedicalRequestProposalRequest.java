package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMedicalRequestProposalRequest(

    @NotNull
    @Min(1)
    @Max(1440)
    Integer estimatedArrivalMinutes,

    @Min(5)
    @Max(120)
    Integer validityMinutes,

    @Size(max = 500)
    String message

) {
}
