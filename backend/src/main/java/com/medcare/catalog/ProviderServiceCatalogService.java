package com.medcare.catalog;

import com.medcare.shared.exception.DomainException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [AHC-21 / AHC-22 / AHC-23] Provider Clinical Services & Pricing Catalog service,
 * continuous duration slot block calculation, and soft-deactivation (INACTIVE).
 */
@Service
public class ProviderServiceCatalogService {

    private final ClinicalServiceRepository serviceRepository;
    private final ServiceAppointmentRepository appointmentRepository;

    public ProviderServiceCatalogService(
            ClinicalServiceRepository serviceRepository,
            ServiceAppointmentRepository appointmentRepository
    ) {
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public ClinicalServiceResponse createService(CreateServiceCommand command) {
        if (command.durationMinutes() < 5 || command.durationMinutes() > 480) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid Service Duration",
                    "Service duration must be between 5 and 480 minutes."
            );
        }
        if (command.feeAmount() == null || command.feeAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid Consultation Fee",
                    "Consultation fee must be strictly positive."
            );
        }

        ClinicalServiceEntity saved = serviceRepository.save(new ClinicalServiceEntity(
                UUID.randomUUID(),
                command.tenantSlug(),
                command.providerId(),
                command.specialty(),
                command.name(),
                command.durationMinutes(),
                command.feeAmount(),
                command.currency(),
                "ACTIVE"
        ));
        return toServiceResponse(saved);
    }

    @Transactional
    public ClinicalServiceResponse updateService(UUID serviceId, CreateServiceCommand command) {
        if (command.durationMinutes() < 5 || command.durationMinutes() > 480) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid Service Duration",
                    "Service duration must be between 5 and 480 minutes."
            );
        }
        if (command.feeAmount() == null || command.feeAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid Consultation Fee",
                    "Consultation fee must be strictly positive."
            );
        }

        ClinicalServiceEntity entity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        "Service Not Found",
                        "Clinical service not found: " + serviceId
                ));
        entity.setName(command.name());
        entity.setSpecialty(command.specialty());
        entity.setDurationMinutes(command.durationMinutes());
        entity.setFeeAmount(command.feeAmount());
        entity.setCurrency(command.currency());
        ClinicalServiceEntity saved = serviceRepository.save(entity);
        return toServiceResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClinicalServiceResponse> listServices(String providerId, boolean activeOnly) {
        List<ClinicalServiceEntity> entities = activeOnly
                ? serviceRepository.findAllByProviderIdAndStatus(providerId, "ACTIVE")
                : serviceRepository.findAllByProviderId(providerId);
        return entities.stream().map(this::toServiceResponse).toList();
    }

    @Transactional
    public ClinicalServiceResponse updateServiceStatus(String providerId, UUID serviceId, String newStatus) {
        ClinicalServiceEntity entity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        "Service Not Found",
                        "Clinical service not found: " + serviceId
                ));
        entity.setStatus(newStatus.toUpperCase());
        ClinicalServiceEntity saved = serviceRepository.save(entity);
        return toServiceResponse(saved);
    }

    @Transactional
    public ServiceAppointmentResponse bookServiceAppointment(BookAppointmentCommand command) {
        ClinicalServiceEntity service = serviceRepository.findById(command.serviceId())
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        "Service Not Found",
                        "Clinical service not found"
                ));

        if (!"ACTIVE".equalsIgnoreCase(service.getStatus())) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Inactive Service",
                    "Cannot book an appointment for an INACTIVE clinical service."
            );
        }

        Instant endTime = command.startTime().plus(service.getDurationMinutes(), ChronoUnit.MINUTES);
        if (appointmentRepository.existsOverlappingAppointment(command.providerId(), command.startTime(), endTime)) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    "Appointment Slot Overlap",
                    "Overlapping appointment block detected between " + command.startTime() + " and " + endTime
            );
        }

        ServiceAppointmentEntity saved = appointmentRepository.save(new ServiceAppointmentEntity(
                UUID.randomUUID(),
                command.tenantSlug(),
                command.providerId(),
                service.getId(),
                service.getName(),
                command.patientName(),
                command.startTime(),
                endTime,
                service.getDurationMinutes(),
                service.getFeeAmount(),
                "SCHEDULED"
        ));
        return toAppointmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ServiceAppointmentResponse> listAppointments(String providerId) {
        return appointmentRepository.findAllByProviderId(providerId).stream()
                .map(this::toAppointmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceAppointmentResponse getHistoricalAppointment(UUID appointmentId) {
        ServiceAppointmentEntity entity = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        "Appointment Not Found",
                        "Appointment not found: " + appointmentId
                ));
        return toAppointmentResponse(entity);
    }

    private ClinicalServiceResponse toServiceResponse(ClinicalServiceEntity entity) {
        return new ClinicalServiceResponse(
                entity.getId(),
                entity.getProviderId(),
                entity.getName(),
                entity.getSpecialty(),
                entity.getDurationMinutes(),
                entity.getFeeAmount(),
                entity.getCurrency(),
                entity.getStatus()
        );
    }

    private ServiceAppointmentResponse toAppointmentResponse(ServiceAppointmentEntity entity) {
        return new ServiceAppointmentResponse(
                entity.getId(),
                entity.getProviderId(),
                entity.getServiceId(),
                entity.getServiceNameSnapshot(),
                entity.getPatientName(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDurationMinutes(),
                entity.getBilledFee(),
                entity.getStatus()
        );
    }

    public record CreateServiceCommand(
            String tenantSlug,
            String providerId,
            String name,
            String specialty,
            int durationMinutes,
            BigDecimal feeAmount,
            String currency
    ) {}

    public record ClinicalServiceResponse(
            UUID id,
            String providerId,
            String name,
            String specialty,
            int durationMinutes,
            BigDecimal feeAmount,
            String currency,
            String status
    ) {}

    public record BookAppointmentCommand(
            String tenantSlug,
            String providerId,
            UUID serviceId,
            String patientName,
            Instant startTime
    ) {}

    public record ServiceAppointmentResponse(
            UUID appointmentId,
            String providerId,
            UUID serviceId,
            String serviceNameSnapshot,
            String patientName,
            Instant startTime,
            Instant endTime,
            int durationMinutes,
            BigDecimal billedFee,
            String status
    ) {}
}
