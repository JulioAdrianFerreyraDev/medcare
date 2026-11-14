package com.medcare.scheduling;

/**
 * [AHC-8 AC-1] Port for Turnstile / reCAPTCHA anti-bot verification on unauthenticated slot hold requests.
 */
public interface AntiBotVerificationPort {

    boolean verifyToken(String captchaToken, String clientIp);
}
