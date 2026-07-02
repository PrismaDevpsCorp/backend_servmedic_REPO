package pe.prismadev.servmedic.dto;

import java.util.List;

public record SpecialistRegistrationResponse(
    Long userId,
    Long specialistProfileId,
    String roleCode,
    String professionCode,
    String collegeNumber,
    String status,
    String email,
    String fullName,
    String dni,
    List<String> offeredServiceCodes,
    String message
) {
}