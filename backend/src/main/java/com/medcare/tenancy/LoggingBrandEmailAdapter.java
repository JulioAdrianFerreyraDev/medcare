package com.medcare.tenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingBrandEmailAdapter implements BrandEmailNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingBrandEmailAdapter.class);

    @Override
    public void sendActivationEmail(String recipientEmail, String legalBusinessName, String portalUrl) {
        log.info("Dispatched brand activation email to {} for '{}' -> {}", recipientEmail, legalBusinessName, portalUrl);
    }
}
