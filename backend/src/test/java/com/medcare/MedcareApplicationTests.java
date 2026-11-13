package com.medcare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MedcareApplicationTests {

    @Test
    @DisplayName("Should initialize Spring Boot application context cleanly")
    void contextLoads() {
    }
}
