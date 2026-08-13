
package pe.prismadev.servmedic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;
import pe.prismadev.servmedic.repository.MedicalServiceRepository;
import pe.prismadev.servmedic.repository.PatientProfileRepository;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalRequestServiceFinishGuardTest {

    private static final Long REQUEST_ID = 100L;
    private static final Long SPECIALIST_ID = 20L;

    @Mock
    private MedicalRequestRepository medicalRequestRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private MedicalServiceRepository medicalServiceRepository;

    @Mock
    private SpecialistProfileRepository specialistProfileRepository;


    @Mock
    private MedicalRequest medicalRequest;

    @Mock
    private SpecialistProfile specialistProfile;

    private MedicalRequestService service;

    @BeforeEach
    void setUp() {
        service = new MedicalRequestService(
            medicalRequestRepository,
            patientProfileRepository,
            medicalServiceRepository,
            specialistProfileRepository
        );
    }

    @Test
    void finishRejectsRequestWhenAttentionReportIsIncomplete() {
        when(
            medicalRequestRepository.findDetailedByIdForUpdate(
                REQUEST_ID
            )
        ).thenReturn(Optional.of(medicalRequest));

        when(
            medicalRequest.getAcceptedSpecialistProfile()
        ).thenReturn(specialistProfile);

        when(
            specialistProfile.getId()
        ).thenReturn(SPECIALIST_ID);

        when(
            medicalRequest.getStatus()
        ).thenReturn("EN_ATENCION");

        when(
            medicalRequestRepository.countCompleteAttentionReports(
                REQUEST_ID
            )
        ).thenReturn(0L);

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () -> service.finish(
                    REQUEST_ID,
                    SPECIALIST_ID
                )
            );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatusCode()
        );

        assertTrue(
            exception.getReason().contains(
                "DEBE INGRESAR DATOS EN LA FICHA DE ATENCION"
            )
        );

        verify(
            medicalRequest,
            never()
        ).finish();

        verify(
            medicalRequestRepository,
            never()
        ).save(medicalRequest);
    }
}
