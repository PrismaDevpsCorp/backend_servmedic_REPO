package pe.prismadev.servmedic.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.prismadev.servmedic.dto.AttentionReportResponse;
import pe.prismadev.servmedic.dto.SaveAttentionReportRequest;
import pe.prismadev.servmedic.service.MedicalAttentionReportService;

@RestController
@RequestMapping("/api/public/medical-requests")
public class MedicalAttentionReportController {

    private final MedicalAttentionReportService attentionReportService;

    public MedicalAttentionReportController(MedicalAttentionReportService attentionReportService) {
        this.attentionReportService = attentionReportService;
    }

    @PutMapping("/{medicalRequestId}/attention-report")
    public AttentionReportResponse saveReport(
        @PathVariable Long medicalRequestId,
        @Valid @RequestBody SaveAttentionReportRequest request
    ) {
        return attentionReportService.saveReport(medicalRequestId, request);
    }

    @GetMapping("/{medicalRequestId}/attention-report")
    public AttentionReportResponse findReport(
        @PathVariable Long medicalRequestId
    ) {
        return attentionReportService.findByMedicalRequestId(medicalRequestId);
    }
}