package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.SpecialistCommercialProfileResponse;
import pe.prismadev.servmedic.dto.SpecialistPaymentMethodResponse;
import pe.prismadev.servmedic.dto.SpecialistServicePriceResponse;
import pe.prismadev.servmedic.dto.UpdateSpecialistCommercialProfileRequest;
import pe.prismadev.servmedic.dto.UpdateSpecialistServicePriceRequest;
import pe.prismadev.servmedic.entity.PaymentMethod;
import pe.prismadev.servmedic.entity.SpecialistCommercialProfile;
import pe.prismadev.servmedic.entity.SpecialistOfferedService;
import pe.prismadev.servmedic.entity.SpecialistPaymentMethod;
import pe.prismadev.servmedic.entity.SpecialistProfile;
import pe.prismadev.servmedic.repository.PaymentMethodRepository;
import pe.prismadev.servmedic.repository.SpecialistCommercialProfileRepository;
import pe.prismadev.servmedic.repository.SpecialistOfferedServiceRepository;
import pe.prismadev.servmedic.repository.SpecialistPaymentMethodRepository;
import pe.prismadev.servmedic.repository.SpecialistProfileRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SpecialistCommercialProfileService {

    private final SpecialistProfileRepository specialistProfileRepository;
    private final SpecialistCommercialProfileRepository commercialProfileRepository;
    private final SpecialistOfferedServiceRepository offeredServiceRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final SpecialistPaymentMethodRepository specialistPaymentMethodRepository;

    public SpecialistCommercialProfileService(
        SpecialistProfileRepository specialistProfileRepository,
        SpecialistCommercialProfileRepository commercialProfileRepository,
        SpecialistOfferedServiceRepository offeredServiceRepository,
        PaymentMethodRepository paymentMethodRepository,
        SpecialistPaymentMethodRepository specialistPaymentMethodRepository
    ) {
        this.specialistProfileRepository = specialistProfileRepository;
        this.commercialProfileRepository = commercialProfileRepository;
        this.offeredServiceRepository = offeredServiceRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.specialistPaymentMethodRepository = specialistPaymentMethodRepository;
    }

    @Transactional
    public SpecialistCommercialProfileResponse getProfile(
        Long specialistProfileId
    ) {
        SpecialistProfile specialist = findSpecialist(specialistProfileId);
        SpecialistCommercialProfile profile = findOrCreateProfile(specialist);

        return buildResponse(specialist, profile);
    }

    @Transactional
    public SpecialistCommercialProfileResponse updateProfile(
        Long specialistProfileId,
        UpdateSpecialistCommercialProfileRequest request
    ) {
        SpecialistProfile specialist = findSpecialist(specialistProfileId);
        SpecialistCommercialProfile profile = findOrCreateProfile(specialist);

        String mobilityPolicy = normalizeMobilityPolicy(
            request.mobilityPolicy()
        );

        BigDecimal mobilityReferenceAmount = validateMobility(
            mobilityPolicy,
            request.mobilityReferenceAmount()
        );

        List<SpecialistOfferedService> offeredServices =
            offeredServiceRepository.findDetailedBySpecialistProfileId(
                specialistProfileId
            );

        validateAndUpdateServices(
            offeredServices,
            request.services()
        );

        List<PaymentMethod> paymentMethods =
            paymentMethodRepository.findAllByActiveTrueOrderByIdAsc();

        updatePaymentMethods(
            specialist,
            paymentMethods,
            request.paymentMethodCodes()
        );

        profile.updateCommercialData(
            mobilityPolicy,
            mobilityReferenceAmount,
            cleanNullable(request.commercialNotes())
        );

        profile.setActive(request.active());

        commercialProfileRepository.save(profile);
        offeredServiceRepository.saveAll(offeredServices);

        return buildResponse(specialist, profile);
    }

    private SpecialistProfile findSpecialist(Long specialistProfileId) {
        if (specialistProfileId == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "El token no contiene un perfil de especialista."
            );
        }

        return specialistProfileRepository
            .findDetailedById(specialistProfileId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil de especialista no encontrado: "
                    + specialistProfileId
            ));
    }

    private SpecialistCommercialProfile findOrCreateProfile(
        SpecialistProfile specialist
    ) {
        return commercialProfileRepository
            .findDetailedBySpecialistProfileId(specialist.getId())
            .orElseGet(() -> commercialProfileRepository.save(
                new SpecialistCommercialProfile(specialist)
            ));
    }

    private void validateAndUpdateServices(
        List<SpecialistOfferedService> offeredServices,
        List<UpdateSpecialistServicePriceRequest> requests
    ) {
        if (offeredServices.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El especialista no tiene servicios ofrecidos configurados."
            );
        }

        Map<Long, SpecialistOfferedService> offeredServicesById =
            offeredServices.stream()
                .collect(Collectors.toMap(
                    SpecialistOfferedService::getId,
                    Function.identity()
                ));

        Set<Long> processedIds = new HashSet<>();
        int activeServiceCount = 0;

        for (UpdateSpecialistServicePriceRequest item : requests) {
            if (!processedIds.add(item.offeredServiceId())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No debe repetir servicios ofrecidos: "
                        + item.offeredServiceId()
                );
            }

            SpecialistOfferedService offeredService =
                offeredServicesById.get(item.offeredServiceId());

            if (offeredService == null) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El servicio ofrecido no pertenece al especialista: "
                        + item.offeredServiceId()
                );
            }

            BigDecimal normalizedPrice = item.basePrice()
                .setScale(2, RoundingMode.HALF_UP);

            if (normalizedPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El precio base debe ser mayor que cero."
                );
            }

            offeredService.setBasePrice(normalizedPrice);
            offeredService.setActive(item.active());

            if (item.active()) {
                activeServiceCount++;
            }
        }

        if (processedIds.size() != offeredServicesById.size()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debe enviar todos los servicios ofrecidos del especialista."
            );
        }

        if (activeServiceCount == 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debe mantener al menos un servicio activo."
            );
        }
    }

    private void updatePaymentMethods(
        SpecialistProfile specialist,
        List<PaymentMethod> availablePaymentMethods,
        List<String> requestedCodes
    ) {
        Map<String, PaymentMethod> availableByCode =
            availablePaymentMethods.stream()
                .collect(Collectors.toMap(
                    paymentMethod ->
                        normalizePaymentMethodCode(paymentMethod.getCode()),
                    Function.identity(),
                    (first, second) -> first,
                    LinkedHashMap::new
                ));

        Set<String> normalizedCodes = new LinkedHashSet<>();

        for (String requestedCode : requestedCodes) {
            String normalizedCode = normalizePaymentMethodCode(
                requestedCode
            );

            if (!normalizedCodes.add(normalizedCode)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No debe repetir mÃ©todos de pago: "
                        + normalizedCode
                );
            }

            if (!availableByCode.containsKey(normalizedCode)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "MÃ©todo de pago no disponible: "
                        + normalizedCode
                );
            }
        }

        if (normalizedCodes.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debe seleccionar al menos un mÃ©todo de pago."
            );
        }

        List<SpecialistPaymentMethod> existingRelations =
            specialistPaymentMethodRepository
                .findDetailedBySpecialistProfileId(specialist.getId());

        Map<Long, SpecialistPaymentMethod> existingByPaymentMethodId =
            existingRelations.stream()
                .collect(Collectors.toMap(
                    relation -> relation.getPaymentMethod().getId(),
                    Function.identity()
                ));

        List<SpecialistPaymentMethod> relationsToSave =
            new ArrayList<>(existingRelations);

        for (SpecialistPaymentMethod relation : existingRelations) {
            String code = normalizePaymentMethodCode(
                relation.getPaymentMethod().getCode()
            );

            relation.setActive(normalizedCodes.contains(code));
        }

        for (String code : normalizedCodes) {
            PaymentMethod paymentMethod = availableByCode.get(code);

            SpecialistPaymentMethod existing =
                existingByPaymentMethodId.get(paymentMethod.getId());

            if (existing != null) {
                existing.setActive(true);
                continue;
            }

            SpecialistPaymentMethod newRelation =
                new SpecialistPaymentMethod(
                    specialist,
                    paymentMethod
                );

            relationsToSave.add(newRelation);
        }

        specialistPaymentMethodRepository.saveAll(relationsToSave);
    }

    private BigDecimal validateMobility(
        String mobilityPolicy,
        BigDecimal mobilityReferenceAmount
    ) {
        if ("SEPARATE".equals(mobilityPolicy)) {
            if (
                mobilityReferenceAmount == null
                || mobilityReferenceAmount.compareTo(BigDecimal.ZERO) <= 0
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La movilidad separada requiere un monto mayor que cero."
                );
            }

            return mobilityReferenceAmount.setScale(
                2,
                RoundingMode.HALF_UP
            );
        }

        if (mobilityReferenceAmount != null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La polÃ­tica "
                    + mobilityPolicy
                    + " no debe registrar un monto de movilidad."
            );
        }

        return null;
    }

    private String normalizeMobilityPolicy(String mobilityPolicy) {
        String normalized = mobilityPolicy == null
            ? ""
            : mobilityPolicy.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "INCLUDED", "SEPARATE", "NOT_AVAILABLE" -> normalized;
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "PolÃ­tica de movilidad no permitida: "
                    + mobilityPolicy
            );
        };
    }

    private String normalizePaymentMethodCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El cÃ³digo del mÃ©todo de pago es obligatorio."
            );
        }

        return code.trim().toUpperCase(Locale.ROOT);
    }

    private SpecialistCommercialProfileResponse buildResponse(
        SpecialistProfile specialist,
        SpecialistCommercialProfile profile
    ) {
        List<SpecialistOfferedService> offeredServices =
            offeredServiceRepository.findDetailedBySpecialistProfileId(
                specialist.getId()
            );

        List<SpecialistServicePriceResponse> serviceResponses =
            offeredServices.stream()
                .map(offeredService ->
                    new SpecialistServicePriceResponse(
                        offeredService.getId(),
                        offeredService.getMedicalService().getId(),
                        offeredService.getMedicalService().getCode(),
                        offeredService.getMedicalService().getName(),
                        offeredService
                            .getMedicalService()
                            .isRequiresPrescription(),
                        offeredService.getBasePrice(),
                        offeredService.isActive()
                    )
                )
                .toList();

        List<SpecialistPaymentMethod> configuredPaymentMethods =
            specialistPaymentMethodRepository
                .findDetailedBySpecialistProfileId(specialist.getId());

        Set<Long> selectedPaymentMethodIds =
            configuredPaymentMethods.stream()
                .filter(SpecialistPaymentMethod::isActive)
                .map(relation ->
                    relation.getPaymentMethod().getId()
                )
                .collect(Collectors.toSet());

        List<SpecialistPaymentMethodResponse> paymentMethodResponses =
            paymentMethodRepository
                .findAllByActiveTrueOrderByIdAsc()
                .stream()
                .map(paymentMethod ->
                    new SpecialistPaymentMethodResponse(
                        paymentMethod.getId(),
                        paymentMethod.getCode(),
                        paymentMethod.getName(),
                        paymentMethod.isRequiresVoucher(),
                        selectedPaymentMethodIds.contains(
                            paymentMethod.getId()
                        )
                    )
                )
                .toList();

        return new SpecialistCommercialProfileResponse(
            specialist.getId(),
            specialist.getProfession().getCode(),
            specialist.getProfession().getName(),
            profile.getMobilityPolicy(),
            profile.getMobilityReferenceAmount(),
            profile.getCommercialNotes(),
            profile.isActive(),
            serviceResponses,
            paymentMethodResponses
        );
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank()
            ? null
            : value.trim();
    }
}