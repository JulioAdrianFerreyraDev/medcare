package com.medcare.availability;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPatientReschedulingNotificationAdapter implements PatientReschedulingNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingPatientReschedulingNotificationAdapter.class);

    @Override
    public void notifyPatientOfEmergencyCancellation(
            String tenantSlug,
            UUID appointmentId,
            String patientName,
            String patientContact,
            String providerId,
            String reason
    ) {
        log.info(
                "[Tenant: {}] Automated emergency rescheduling notification sent to {} ({}) for appointment {} with provider {} (Reason: {})",
                tenantSlug,
                patientName,
                patientContact,
                appointmentId,
                providerId,
                reason
        );
    }
}
