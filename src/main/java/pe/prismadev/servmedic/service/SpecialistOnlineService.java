package pe.prismadev.servmedic.service;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.NearbyMedicalRequestResponse;
import pe.prismadev.servmedic.dto.SpecialistOnlineResponse;
import pe.prismadev.servmedic.dto.UpdateSpecialistLocationRequest;
import pe.prismadev.servmedic.entity.MedicalRequest;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.entity.UserAccount;
import pe.prismadev.servmedic.repository.MedicalRequestRepository;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SpecialistOnlineService {

    private static final String GEO_KEY = "servmedic:specialists:online:geo";
    private static final String DETAIL_KEY_PREFIX = "servmedic:specialists:online:detail:";
    private static final Duration ONLINE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final SpecialistProfileRepository specialistProfileRepository;
    private final MedicalRequestRepository medicalRequestRepository;

    public SpecialistOnlineService(
        StringRedisTemplate redisTemplate,
        JdbcTemplate jdbcTemplate,
        SpecialistProfileRepository specialistProfileRepository,
        MedicalRequestRepository medicalRequestRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.specialistProfileRepository = specialistProfileRepository;
        this.medicalRequestRepository = medicalRequestRepository;
    }

    @Transactional(readOnly = true)
    public SpecialistOnlineResponse updateOnlineLocation(
        Long specialistProfileId,
        UpdateSpecialistLocationRequest request
    ) {
        SpecialistProfile specialist = findAndValidateSpecialist(specialistProfileId);

        String member = memberName(specialistProfileId);

        redisTemplate.opsForGeo().add(
            GEO_KEY,
            new Point(request.longitude().doubleValue(), request.latitude().doubleValue()),
            member
        );

        redisTemplate.opsForValue().set(
            detailKey(specialistProfileId),
            specialist.getProfession().getCode(),
            ONLINE_TTL
        );

        return new SpecialistOnlineResponse(
            specialist.getId(),
            fullName(specialist.getUserAccount()),
            specialist.getProfession().getCode(),
            true,
            request.latitude(),
            request.longitude(),
            LocalDateTime.now(),
            "Especialista online y ubicacion actualizada en Redis."
        );
    }

    public SpecialistOnlineResponse goOffline(Long specialistProfileId) {
        SpecialistProfile specialist = specialistProfileRepository.findDetailedById(specialistProfileId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de especialista no encontrado: " + specialistProfileId
            ));

        redisTemplate.opsForGeo().remove(GEO_KEY, memberName(specialistProfileId));
        redisTemplate.delete(detailKey(specialistProfileId));

        return new SpecialistOnlineResponse(
            specialist.getId(),
            fullName(specialist.getUserAccount()),
            specialist.getProfession().getCode(),
            false,
            null,
            null,
            LocalDateTime.now(),
            "Especialista marcado como offline en Redis."
        );
    }

    @Transactional(readOnly = true)
    public List<NearbyMedicalRequestResponse> findNearbyPendingRequests(
        Long specialistProfileId,
        BigDecimal radiusKm
    ) {
        SpecialistProfile specialist = findAndValidateSpecialist(specialistProfileId);
        validateRadius(radiusKm);

        String member = memberName(specialistProfileId);

        List<Point> positions = redisTemplate.opsForGeo().position(GEO_KEY, member);

        if (positions == null || positions.isEmpty() || positions.get(0) == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El especialista no tiene ubicacion activa en Redis. Primero debe ponerse online."
            );
        }

        Point specialistPoint = positions.get(0);
        BigDecimal longitude = BigDecimal.valueOf(specialistPoint.getX());
        BigDecimal latitude = BigDecimal.valueOf(specialistPoint.getY());

        List<NearbyRequestRow> nearbyRows = findNearbyRows(
            specialist.getProfession().getCode(),
            latitude,
            longitude,
            radiusKm
        );

        return nearbyRows.stream()
            .map(row -> {
                MedicalRequest request = medicalRequestRepository.findDetailedById(row.medicalRequestId())
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se pudo recuperar solicitud cercana: " + row.medicalRequestId()
                    ));

                return toNearbyResponse(request, row.distanceKm());
            })
            .toList();
    }

    private SpecialistProfile findAndValidateSpecialist(Long specialistProfileId) {
        SpecialistProfile specialist = specialistProfileRepository.findDetailedById(specialistProfileId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de especialista no encontrado: " + specialistProfileId
            ));

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

        return specialist;
    }

    private void validateRadius(BigDecimal radiusKm) {
        if (radiusKm == null || radiusKm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El radio debe ser mayor a cero."
            );
        }

        if (radiusKm.compareTo(new BigDecimal("50")) > 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El radio maximo permitido para esta prueba es 50 km."
            );
        }
    }

    private List<NearbyRequestRow> findNearbyRows(
        String professionCode,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal radiusKm
    ) {
        String sql = """
            SELECT
                mr.id AS medical_request_id,
                ROUND((
                    ST_Distance(
                        mr.location,
                        ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                    ) / 1000.0
                )::numeric, 2) AS distance_km
            FROM medical_requests mr
            INNER JOIN medical_services ms ON ms.id = mr.medical_service_id
            INNER JOIN professions p ON p.id = ms.profession_id
            WHERE mr.status = 'PENDING'
              AND p.code = ?
              AND ST_DWithin(
                    mr.location,
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                    ?
              )
            ORDER BY distance_km ASC, mr.created_at ASC
            LIMIT 20
        """;

        BigDecimal radiusMeters = radiusKm.multiply(new BigDecimal("1000"));

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new NearbyRequestRow(
                rs.getLong("medical_request_id"),
                rs.getBigDecimal("distance_km")
            ),
            longitude,
            latitude,
            professionCode,
            longitude,
            latitude,
            radiusMeters
        );
    }

    private NearbyMedicalRequestResponse toNearbyResponse(MedicalRequest request, BigDecimal distanceKm) {
        UserAccount patientUser = request.getPatientProfile().getUserAccount();

        return new NearbyMedicalRequestResponse(
            request.getId(),
            request.getRequestCode(),
            request.getPatientProfile().getId(),
            fullName(patientUser),
            request.getMedicalService().getCode(),
            request.getMedicalService().getName(),
            request.getMedicalService().getProfession().getCode(),
            request.getStatus(),
            request.getAddressText(),
            request.getLatitude(),
            request.getLongitude(),
            distanceKm,
            request.getEstimatedAmount(),
            request.getCreatedAt()
        );
    }

    private String memberName(Long specialistProfileId) {
        return "specialist:" + specialistProfileId;
    }

    private String detailKey(Long specialistProfileId) {
        return DETAIL_KEY_PREFIX + specialistProfileId;
    }

    private String fullName(UserAccount user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private record NearbyRequestRow(
        Long medicalRequestId,
        BigDecimal distanceKm
    ) {
    }
}