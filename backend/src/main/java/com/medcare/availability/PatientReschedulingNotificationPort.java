package com.medcare.availability;

import java.util.UUID;

/**
 * [AHC-7 AC-2] Port for dispatching automated rescheduling notifications to affected patients.
 */
public interface PatientReschedulingNotificationPort {

    void notifyPatientOfEmergencyCancellation(
            String tenantSlug,
            UUID appointmentId,
            String patientName,
            String patientContact,
            String providerId,
            String reason
    );
}
