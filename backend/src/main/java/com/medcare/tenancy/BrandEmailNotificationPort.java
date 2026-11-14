package com.medcare.tenancy;

public interface BrandEmailNotificationPort {

    void sendActivationEmail(String recipientEmail, String legalBusinessName, String portalUrl);
}
