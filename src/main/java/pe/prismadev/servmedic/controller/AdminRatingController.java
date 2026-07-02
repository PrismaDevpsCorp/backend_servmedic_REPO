package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.prismadev.servmedic.dto.AdminRatingResponse;
import pe.prismadev.servmedic.dto.AdminRatingSummaryResponse;
import pe.prismadev.servmedic.service.AdminRatingService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ratings")
public class AdminRatingController {

    private final AdminRatingService adminRatingService;

    public AdminRatingController(AdminRatingService adminRatingService) {
        this.adminRatingService = adminRatingService;
    }

    @GetMapping
    public List<AdminRatingResponse> listRatings(
        @RequestParam(required = false) String ratingType
    ) {
        return adminRatingService.listRatings(ratingType);
    }

    @GetMapping("/summary")
    public AdminRatingSummaryResponse getSummary(
        @RequestParam(required = false) String ratingType
    ) {
        return adminRatingService.getSummary(ratingType);
    }
}