package org.fireflyframework.application.controller;

import org.fireflyframework.application.context.AppConfig;
import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.resolver.ConfigResolver;
import org.fireflyframework.application.resolver.ContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AbstractResourceController Tests")
@ExtendWith(MockitoExtension.class)
class AbstractResourceControllerTest {
    
    @Mock
    private ContextResolver contextResolver;
    
    @Mock
    private ConfigResolver configResolver;
    
    @Mock
    private ServerWebExchange exchange;
    
    private TestResourceController controller;
    
    private UUID testPartyId;
    private UUID testTenantId;
    private UUID testContractId;
    private UUID testProductId;
    
    @BeforeEach
    void setUp() {
        controller = new TestResourceController();
        ReflectionTestUtils.setField(controller, "contextResolver", contextResolver);
        ReflectionTestUtils.setField(controller, "configResolver", configResolver);
        
        testPartyId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
        testContractId = UUID.randomUUID();
        testProductId = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("Should validate valid context (both IDs)")
    void shouldValidateValidContext() {
        UUID contractId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        
        assertDoesNotThrow(() -> controller.testRequireContext(contractId, productId));
    }
    
    @Test
    @DisplayName("Should throw exception for null contract ID")
    void shouldThrowExceptionForNullContractId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> controller.testRequireContext(null, testProductId)
        );
        
        assertTrue(exception.getMessage().contains("contractId is required"));
    }
    
    @Test
    @DisplayName("Should throw exception for null product ID")
    void shouldThrowExceptionForNullProductId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> controller.testRequireContext(testContractId, null)
        );
        
        assertTrue(exception.getMessage().contains("productId is required"));
    }
    
    @Test
    @DisplayName("Should log operation with resource context")
    void shouldLogOperationWithResourceContext() {
        UUID contractId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        
        assertDoesNotThrow(() -> controller.testLogOperation(contractId, productId, "testOperation"));
    }
    
    @Test
    @DisplayName("Should handle null operation name in logging")
    void shouldHandleNullOperationNameInLogging() {
        UUID contractId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        
        assertDoesNotThrow(() -> controller.testLogOperation(contractId, productId, null));
    }
    
    @Test
    @DisplayName("Should resolve resource context (contract + product)")
    void shouldResolveResourceContext() {
        // Given
        AppContext appContext = AppContext.builder()
                .partyId(testPartyId)
                .tenantId(testTenantId)
                .contractId(testContractId)
                .productId(testProductId)
                .roles(Set.of("transaction:viewer"))
                .permissions(Set.of("transaction:read"))
                .build();
        
        AppConfig appConfig = AppConfig.builder()
                .tenantId(testTenantId)
                .tenantName("Test Tenant")
                .build();
        
        when(contextResolver.resolveContext(any(ServerWebExchange.class), eq(testContractId), eq(testProductId)))
                .thenReturn(Mono.just(appContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.just(appConfig));
        
        // When
        Mono<ApplicationExecutionContext> result = controller.testResolveExecutionContext(
                exchange, testContractId, testProductId);
        
        // Then
        StepVerifier.create(result)
                .assertNext(ctx -> {
                    assertThat(ctx).isNotNull();
                    assertThat(ctx.getContext().getPartyId()).isEqualTo(testPartyId);
                    assertThat(ctx.getContext().getTenantId()).isEqualTo(testTenantId);
                    assertThat(ctx.getContext().getContractId()).isEqualTo(testContractId);
                    assertThat(ctx.getContext().getProductId()).isEqualTo(testProductId);
                })
                .verifyComplete();
        
        verify(contextResolver).resolveContext(eq(exchange), eq(testContractId), eq(testProductId));
        verify(configResolver).resolveConfig(testTenantId);
    }
    
    @Test
    @DisplayName("Should throw exception when contract ID is null in context resolution")
    void shouldThrowExceptionWhenContractIdIsNullInContextResolution() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            controller.testResolveExecutionContext(exchange, null, testProductId).block();
        });
    }
    
    @Test
    @DisplayName("Should throw exception when product ID is null in context resolution")
    void shouldThrowExceptionWhenProductIdIsNullInContextResolution() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            controller.testResolveExecutionContext(exchange, testContractId, null).block();
        });
    }
    
    /**
     * Concrete test implementation of AbstractResourceController
     * to expose protected methods for testing
     */
    static class TestResourceController extends AbstractResourceController {
        
        public void testRequireContext(UUID contractId, UUID productId) {
            requireContext(contractId, productId);
        }
        
        public void testLogOperation(UUID contractId, UUID productId, String operation) {
            logOperation(contractId, productId, operation);
        }
        
        public Mono<ApplicationExecutionContext> testResolveExecutionContext(
                ServerWebExchange exchange, UUID contractId, UUID productId) {
            return resolveExecutionContext(exchange, contractId, productId);
        }
    }
}
