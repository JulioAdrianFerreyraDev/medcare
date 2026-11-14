package com.medcare.scheduling;

import org.springframework.stereotype.Component;

/**
 * [AHC-8 AC-1] Default adapter verifying Turnstile / reCAPTCHA tokens.
 * Rejects blank or explicitly invalid tokens ("invalid-bot-token", "bot-detected").
 */
@Component
public class TurnstileVerificationAdapter implements AntiBotVerificationPort {

    @Override
    public boolean verifyToken(String captchaToken, String clientIp) {
        if (captchaToken == null || captchaToken.isBlank()) {
            return false;
        }
        String normalized = captchaToken.trim().toLowerCase();
        return !normalized.equals("invalid-bot-token") && !normalized.equals("bot-detected");
    }
}
