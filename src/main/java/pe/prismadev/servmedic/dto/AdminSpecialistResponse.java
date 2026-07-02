package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;

public record AdminSpecialistResponse(
    Long specialistProfileId,
    Long userId,
    String fullName,
    String email,
    String dni,
    String mobilePhone,
    String professionCode,
    String professionName,
    String collegeNumber,
    String status,
    boolean available,
    BigDecimal ratingAverage,
    int ratingCount
) {
}