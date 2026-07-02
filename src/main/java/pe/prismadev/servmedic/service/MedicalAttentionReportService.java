package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.AttentionReportResponse;
import pe.prismadev.servmedic.dto.SaveAttentionReportRequest;
import pe.prismadev.servmedic.entity.MedicalAttentionReport;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalAttentionReportRepository;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

@Service
public class MedicalAttentionReportService {

    private final MedicalAttentionReportRepository attentionReportRepository;
    private final MedicalRequestRepository medicalRequestRepository;
    private final SpecialistProfileRepository specialistProfileRepository;

    public MedicalAttentionReportService(
        MedicalAttentionReportRepository attentionReportRepository,
        MedicalRequestRepository medicalRequestRepository,
        SpecialistProfileRepository specialistProfileRepository
    ) {
        this.attentionReportRepository = attentionReportRepository;
        this.medicalRequestRepository = medicalRequestRepository;
        this.specialistProfileRepository = specialistProfileRepository;
    }

    @Transactional
    public AttentionReportResponse saveReport(Long medicalRequestId, SaveAttentionReportRequest request) {
        MedicalRequest medicalRequest = medicalRequestRepository.findDetailedById(medicalRequestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + medicalRequestId
            ));

        SpecialistProfile specialist = specialistProfileRepository.findDetailedById(request.specialistProfileId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de especialista no encontrado: " + request.specialistProfileId()
            ));

        validateCanSaveReport(medicalRequest, specialist);

        MedicalAttentionReport report = attentionReportRepository.findDetailedByMedicalRequestId(medicalRequestId)
            .orElseGet(MedicalAttentionReport::new);

        report.setMedicalRequest(medicalRequest);
        report.setSpecialistProfile(specialist);
        report.setClinicalObservations(request.clinicalObservations().trim());
        report.setDiagnosticImpression(cleanNullable(request.diagnosticImpression()));
        report.setRecommendations(request.recommendations().trim());
        report.setIndications(cleanNullable(request.indications()));
        report.setVitalSigns(cleanNullable(request.vitalSigns()));
        report.setAttachmentUrl(cleanNullable(request.attachmentUrl()));
        report.touchUpdatedAt();

        MedicalAttentionReport saved = attentionReportRepository.save(report);

        MedicalAttentionReport detailed = attentionReportRepository.findDetailedByMedicalRequestId(saved.getMedicalRequest().getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo recuperar la ficha de atencion."
            ));

        return toResponse(detailed);
    }

    @Transactional(readOnly = true)
    public AttentionReportResponse findByMedicalRequestId(Long medicalRequestId) {
        MedicalAttentionReport report = attentionReportRepository.findDetailedByMedicalRequestId(medicalRequestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Ficha de atencion no encontrada para la solicitud: " + medicalRequestId
            ));

        return toResponse(report);
    }

    private void validateCanSaveReport(MedicalRequest medicalRequest, SpecialistProfile specialist) {
        if (medicalRequest.getAcceptedSpecialistProfile() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud no tiene especialista asignado."
            );
        }

        if (!medicalRequest.getAcceptedSpecialistProfile().getId().equals(specialist.getId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo el especialista asignado puede registrar la ficha de atencion."
            );
        }

        if (!"EN_ATENCION".equals(medicalRequest.getStatus()) && !"FINALIZADO".equals(medicalRequest.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La ficha solo puede registrarse cuando la solicitud esta EN_ATENCION o FINALIZADO. Estado actual: "
                    + medicalRequest.getStatus()
            );
        }
    }

    private AttentionReportResponse toResponse(MedicalAttentionReport report) {
        MedicalRequest request = report.getMedicalRequest();
        UserAccount patientUser = request.getPatientProfile().getUserAccount();
        UserAccount specialistUser = report.getSpecialistProfile().getUserAccount();

        return new AttentionReportResponse(
            report.getId(),
            request.getId(),
            request.getRequestCode(),
            request.getStatus(),
            request.getPatientProfile().getId(),
            patientUser.getFirstName() + " " + patientUser.getLastName(),
            report.getSpecialistProfile().getId(),
            specialistUser.getFirstName() + " " + specialistUser.getLastName(),
            request.getMedicalService().getCode(),
            request.getMedicalService().getName(),
            report.getClinicalObservations(),
            report.getDiagnosticImpression(),
            report.getRecommendations(),
            report.getIndications(),
            report.getVitalSigns(),
            report.getAttachmentUrl(),
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}