package pe.prismadev.servmedic.dto;

public record AdminPatientResponse(
    Long patientProfileId,
    Long userId,
    String fullName,
    String email,
    String dni,
    String mobilePhone,
    Long totalMedicalRequests,
    String lastRequestCode,
    String lastRequestStatus,
    String lastRequestCreatedAt
) {
}