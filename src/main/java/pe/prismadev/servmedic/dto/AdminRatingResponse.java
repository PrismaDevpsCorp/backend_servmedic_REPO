package pe.prismadev.servmedic.dto;

public record AdminRatingResponse(
    Long ratingId,
    Long medicalRequestId,
    String requestCode,
    String requestStatus,

    String raterRole,
    String ratingType,

    Long patientProfileId,
    String patientFullName,
    String patientEmail,
    String patientMobilePhone,

    Long specialistProfileId,
    String specialistFullName,
    String specialistEmail,
    String specialistMobilePhone,

    Integer score,
    String comment,
    String createdAt
) {
}