package pe.prismadev.servmedic.dto;

public record DashboardStatusCountResponse(
    String status,
    Long total
) {
}