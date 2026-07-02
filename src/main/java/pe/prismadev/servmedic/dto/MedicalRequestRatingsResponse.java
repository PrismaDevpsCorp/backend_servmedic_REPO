package pe.prismadev.servmedic.dto;

import java.util.List;

public record MedicalRequestRatingsResponse(
    Long medicalRequestId,
    String requestCode,
    String requestStatus,
    List<RatingResponse> ratings
) {
}