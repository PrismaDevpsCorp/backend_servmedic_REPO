package pe.prismadev.servmedic.service;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.MedicalRequestResponse;
import pe.prismadev.servmedic.dto.RequestOfferResponse;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;
import pe.prismadev.servmedic.repository.SpecialistOfferedServiceRepository;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MedicalRequestOfferService {

    private static final String OFFER_KEY_PREFIX = "servmedic:request-offer:";
    private static final String GEO_KEY = "servmedic:specialists:online:geo";

    private final StringRedisTemplate redisTemplate;
    private final MedicalRequestRepository medicalRequestRepository;
    private final SpecialistProfileRepository specialistProfileRepository;
    private final SpecialistOfferedServiceRepository specialistOfferedServiceRepository;
    private final MedicalRequestService medicalRequestService;

    public MedicalRequestOfferService(
        StringRedisTemplate redisTemplate,
        MedicalRequestRepository medicalRequestRepository,
        SpecialistProfileRepository specialistProfileRepository,
        SpecialistOfferedServiceRepository specialistOfferedServiceRepository,
        MedicalRequestService medicalRequestService
    ) {
        this.redisTemplate = redisTemplate;
        this.medicalRequestRepository = medicalRequestRepository;
        this.specialistProfileRepository = specialistProfileRepository;
        this.specialistOfferedServiceRepository = specialistOfferedServiceRepository;
        this.medicalRequestService = medicalRequestService;
    }

    public RequestOfferResponse offerRequestToSpecialist(Long medicalRequestId, Long specialistProfileId, int ttlSeconds) {
        validateTtl(ttlSeconds);

        MedicalRequest request = findRequest(medicalRequestId);
        SpecialistProfile specialist = findSpecialist(specialistProfileId);

        validateRequestCanBeOffered(request);
        validateSpecialistCanReceiveOffer(request, specialist);
        validateSpecialistIsOnline(specialistProfileId);

        String key = offerKey(medicalRequestId, specialistProfileId);
        String value = request.getRequestCode() + "|" + specialist.getProfession().getCode();

        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));

        return toResponse(
            request,
            specialist,
            true,
            ttlSeconds,
            LocalDateTime.now().plusSeconds(ttlSeconds),
            "Alerta enviada al especialista con TTL en Redis."
        );
    }

    public RequestOfferResponse getOfferStatus(Long medicalRequestId, Long specialistProfileId) {
        MedicalRequest request = findRequest(medicalRequestId);
        SpecialistProfile specialist = findSpecialist(specialistProfileId);

        String key = offerKey(medicalRequestId, specialistProfileId);
        Boolean exists = redisTemplate.hasKey(key);

        if (exists == null || !exists) {
            return toResponse(
                request,
                specialist,
                false,
                0,
                null,
                "No existe alerta activa para esta solicitud y especialista."
            );
        }

        long ttl = remainingTtlSeconds(key);

        return toResponse(
            request,
            specialist,
            true,
            ttl,
            LocalDateTime.now().plusSeconds(ttl),
            "Alerta activa en Redis."
        );
    }

    public MedicalRequestResponse acceptOffer(Long medicalRequestId, Long specialistProfileId) {
        String key = offerKey(medicalRequestId, specialistProfileId);

        Boolean exists = redisTemplate.hasKey(key);

        if (exists == null || !exists) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La alerta de aceptacion expiro o no existe. Debe generarse una nueva alerta."
            );
        }

        MedicalRequestResponse response = medicalRequestService.acceptRequest(medicalRequestId, specialistProfileId);

        redisTemplate.delete(key);

        return response;
    }

    private MedicalRequest findRequest(Long medicalRequestId) {
        return medicalRequestRepository.findDetailedById(medicalRequestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud medica no encontrada: " + medicalRequestId
            ));
    }

    private SpecialistProfile findSpecialist(Long specialistProfileId) {
        return specialistProfileRepository.findDetailedById(specialistProfileId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de especialista no encontrado: " + specialistProfileId
            ));
    }

    private void validateRequestCanBeOffered(MedicalRequest request) {
        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud no esta disponible para alerta. Estado actual: " + request.getStatus()
            );
        }
    }

    private void validateSpecialistCanReceiveOffer(MedicalRequest request, SpecialistProfile specialist) {
        if (!"ACTIVE".equals(specialist.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El especialista no esta activo. Estado actual: " + specialist.getStatus()
            );
        }

        if (!specialist.isAvailable()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El especialista no esta disponible."
            );
        }

        String requestProfession = request.getMedicalService().getProfession().getCode();
        String specialistProfession = specialist.getProfession().getCode();

        if (!requestProfession.equals(specialistProfession)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Regla antintrusismo: la solicitud requiere profesion "
                    + requestProfession + " pero el especialista pertenece a " + specialistProfession
            );
        }

        boolean offersService = specialistOfferedServiceRepository
            .existsBySpecialistProfileIdAndMedicalServiceIdAndActiveTrue(
                specialist.getId(),
                request.getMedicalService().getId()
            );

        if (!offersService) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El especialista no tiene configurado este servicio dentro de sus servicios ofrecidos."
            );
        }
    }

    private void validateSpecialistIsOnline(Long specialistProfileId) {
        List<Point> positions = redisTemplate.opsForGeo().position(GEO_KEY, memberName(specialistProfileId));

        if (positions == null || positions.isEmpty() || positions.get(0) == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El especialista no tiene ubicacion activa en Redis. Primero debe ponerse online."
            );
        }
    }

    private void validateTtl(int ttlSeconds) {
        if (ttlSeconds < 5 || ttlSeconds > 120) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El TTL debe estar entre 5 y 120 segundos."
            );
        }
    }

    private long remainingTtlSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        if (ttl == null || ttl < 0) {
            return 0;
        }

        return ttl;
    }

    private RequestOfferResponse toResponse(
        MedicalRequest request,
        SpecialistProfile specialist,
        boolean offerActive,
        long ttlSeconds,
        LocalDateTime expiresAt,
        String message
    ) {
        return new RequestOfferResponse(
            request.getId(),
            request.getRequestCode(),
            request.getStatus(),
            specialist.getId(),
            fullName(specialist.getUserAccount()),
            specialist.getProfession().getCode(),
            request.getMedicalService().getCode(),
            offerActive,
            ttlSeconds,
            expiresAt,
            message
        );
    }

    private String offerKey(Long medicalRequestId, Long specialistProfileId) {
        return OFFER_KEY_PREFIX + medicalRequestId + ":" + specialistProfileId;
    }

    private String memberName(Long specialistProfileId) {
        return "specialist:" + specialistProfileId;
    }

    private String fullName(UserAccount user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}