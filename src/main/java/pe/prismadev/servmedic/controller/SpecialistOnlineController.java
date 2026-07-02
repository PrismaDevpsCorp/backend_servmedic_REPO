package pe.prismadev.servmedic.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.NearbyMedicalRequestResponse;
import pe.prismadev.servmedic.dto.SpecialistOnlineResponse;
import pe.prismadev.servmedic.dto.UpdateSpecialistLocationRequest;
import pe.prismadev.servmedic.service.SpecialistOnlineService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/public/specialists")
public class SpecialistOnlineController {

    private final SpecialistOnlineService specialistOnlineService;

    public SpecialistOnlineController(SpecialistOnlineService specialistOnlineService) {
        this.specialistOnlineService = specialistOnlineService;
    }

    @PatchMapping("/{specialistProfileId}/online-location")
    public SpecialistOnlineResponse updateOnlineLocation(
        @PathVariable Long specialistProfileId,
        @Valid @RequestBody UpdateSpecialistLocationRequest request
    ) {
        return specialistOnlineService.updateOnlineLocation(specialistProfileId, request);
    }

    @DeleteMapping("/{specialistProfileId}/online")
    public SpecialistOnlineResponse goOffline(@PathVariable Long specialistProfileId) {
        return specialistOnlineService.goOffline(specialistProfileId);
    }

    @GetMapping("/{specialistProfileId}/nearby-pending-requests")
    public List<NearbyMedicalRequestResponse> findNearbyPendingRequests(
        @PathVariable Long specialistProfileId,
        @RequestParam(defaultValue = "5") BigDecimal radiusKm
    ) {
        return specialistOnlineService.findNearbyPendingRequests(specialistProfileId, radiusKm);
    }
}