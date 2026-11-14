package com.medcare.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medcare.shared.exception.DomainException;
import com.medcare.tenancy.BrandOnboardingService.BrandOnboardingCommand;
import com.medcare.tenancy.BrandOnboardingService.BrandOnboardingResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * [AHC-12] JUnit 5 Unit Tests for Story AHC-3:
 * Automated Subdomain Provisioning & Brand Onboarding.
 */
@ExtendWith(MockitoExtension.class)
class TenantOnboardingUnitTest {

    @Mock
    private BrandTenantRepository tenantRepository;

    @Mock
    private BrandEmailNotificationPort emailNotificationPort;

    private SubdomainValidator subdomainValidator;
    private SlugSuggestionGenerator slugSuggestionGenerator;
    private BrandOnboardingService brandOnboardingService;

    @BeforeEach
    void setUp() {
        subdomainValidator = new SubdomainValidator();
        slugSuggestionGenerator = new SlugSuggestionGenerator(tenantRepository);
        brandOnboardingService = new BrandOnboardingService(
                tenantRepository,
                subdomainValidator,
                slugSuggestionGenerator,
                emailNotificationPort
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("AC-1: Valid slug 'clinica-irma' should provision brand tenant, enable routing and dispatch activation email")
    void shouldProvisionTenantAndSendActivationEmailWhenSlugIsValidAndUnique() {
        var command = new BrandOnboardingCommand(
                "Clínica Irma S.A. de C.V.",
                "contacto@clinica-irma.com",
                "+525512345678",
                "clinica-irma"
        );
        when(tenantRepository.existsBySlug("clinica-irma")).thenReturn(false);
        when(tenantRepository.save(any(BrandTenantEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        BrandOnboardingResponse result = brandOnboardingService.onboardBrand(command);

        assertThat(result).isNotNull();
        assertThat(result.slug()).isEqualTo("clinica-irma");
        assertThat(result.subdomainHost()).isEqualTo("clinica-irma.medika.com");
        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(tenantRepository).save(any(BrandTenantEntity.class));
        verify(emailNotificationPort).sendActivationEmail(
                eq("contacto@clinica-irma.com"),
                eq("Clínica Irma S.A. de C.V."),
                eq("https://clinica-irma.medika.com")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"api", "admin", "www", "app", "auth", "billing", "root", "support"})
    @DisplayName("AC-1: Reserved system subdomains should be rejected with DomainException")
    void shouldRejectReservedSubdomainSlugs(String reservedSlug) {
        var command = new BrandOnboardingCommand(
                "Medika Internal",
                "admin@medika.com",
                "+525500000000",
                reservedSlug
        );

        assertThatThrownBy(() -> brandOnboardingService.onboardBrand(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(domainEx.getMessage()).contains("reserved");
                });

        verify(tenantRepository, never()).save(any());
        verify(emailNotificationPort, never()).sendActivationEmail(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Clinica-Irma", "clinica_irma", "-clinica", "clinica-", "clinica irma", "ab"})
    @DisplayName("AC-1: Malformed slugs violating lowercase alphanumeric/hyphen regex should be rejected")
    void shouldRejectInvalidSubdomainRegexFormats(String invalidSlug) {
        var command = new BrandOnboardingCommand(
                "Clínica Irma",
                "owner@clinica.com",
                "+525512345678",
                invalidSlug
        );

        assertThatThrownBy(() -> brandOnboardingService.onboardBrand(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Invalid subdomain slug format");

        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC-2: Duplicate slug 'clinica-irma' should throw HTTP 409 Conflict with 3 available slug suggestions")
    void shouldRejectCollisionWith409AndReturnThreeAvailableSlugSuggestions() {
        var command = new BrandOnboardingCommand(
                "Clínica Irma Norte",
                "norte@clinica-irma.com",
                "+525598765432",
                "clinica-irma"
        );
        when(tenantRepository.existsBySlug("clinica-irma")).thenReturn(true);
        when(tenantRepository.existsBySlug("clinica-irma-salud")).thenReturn(false);
        when(tenantRepository.existsBySlug("clinica-irma-med")).thenReturn(false);
        when(tenantRepository.existsBySlug("clinica-irma-clinica")).thenReturn(false);

        assertThatThrownBy(() -> brandOnboardingService.onboardBrand(command))
                .isInstanceOf(SubdomainCollisionException.class)
                .satisfies(ex -> {
                    SubdomainCollisionException collisionEx = (SubdomainCollisionException) ex;
                    assertThat(collisionEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(collisionEx.getSuggestions())
                            .hasSize(3)
                            .containsExactly(
                                    "clinica-irma-salud",
                                    "clinica-irma-med",
                                    "clinica-irma-clinica"
                            );
                });

        verify(tenantRepository, never()).save(any());
        verify(emailNotificationPort, never()).sendActivationEmail(any(), any(), any());
    }

    @Test
    @DisplayName("AC-3: TenantInterceptor should extract 'clinica-irma' from Host header and clear TenantContext on completion")
    void shouldExtractTenantFromHostHeaderAndCleanUpContextAfterCompletion() {
        TenantInterceptor interceptor = new TenantInterceptor(subdomainValidator);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        when(request.getHeader("Host")).thenReturn("clinica-irma.medika.com:8080");

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("clinica-irma");

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("AC-3: TenantInterceptor should ignore reserved subdomains like 'api.medika.com' without binding tenant context")
    void shouldIgnoreReservedSubdomainInHostHeader() {
        TenantInterceptor interceptor = new TenantInterceptor(subdomainValidator);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        when(request.getHeader("Host")).thenReturn("api.medika.com");

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
