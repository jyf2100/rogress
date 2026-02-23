package com.alibaba.apiopenplatform.service.impl;

import com.alibaba.apiopenplatform.dto.result.product.ProductRefResult;
import com.alibaba.apiopenplatform.entity.Gateway;
import com.alibaba.apiopenplatform.repository.GatewayRepository;
import com.alibaba.apiopenplatform.repository.ProductRefRepository;
import com.alibaba.apiopenplatform.service.gateway.GatewayOperator;
import com.alibaba.apiopenplatform.support.consumer.ConsumerAuthConfig;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.gateway.GatewayConfig;
import com.alibaba.apiopenplatform.support.product.ApisixRefConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayServiceImplApisixP0Test {

    @Mock
    private GatewayRepository gatewayRepository;

    @Mock
    private ProductRefRepository productRefRepository;

    @Mock
    private com.alibaba.apiopenplatform.core.security.ContextHolder contextHolder;

    @Mock
    private GatewayOperator gatewayOperator;

    private GatewayServiceImpl gatewayService;

    @BeforeEach
    void setUp() {
        gatewayService = new GatewayServiceImpl(gatewayRepository, productRefRepository, contextHolder);
        ReflectionTestUtils.setField(gatewayService, "gatewayOperators", Map.of(GatewayType.APISIX, gatewayOperator));
    }

    @Test
    void authorizeConsumerShouldUseApisixRefConfig() {
        Gateway gateway = new Gateway();
        gateway.setGatewayId("apisix-1");
        gateway.setGatewayType(GatewayType.APISIX);

        when(gatewayRepository.findByGatewayId("apisix-1")).thenReturn(Optional.of(gateway));
        when(gatewayOperator.authorizeConsumer(eq(gateway), eq("gw-consumer-1"), any()))
                .thenReturn(ConsumerAuthConfig.builder().build());

        ApisixRefConfig apisixRefConfig = new ApisixRefConfig();
        apisixRefConfig.setRouteId("route-1");
        ProductRefResult productRef = new ProductRefResult();
        productRef.setGatewayId("apisix-1");
        productRef.setApisixRefConfig(apisixRefConfig);

        ConsumerAuthConfig result = gatewayService.authorizeConsumer("apisix-1", "gw-consumer-1", productRef);
        assertNotNull(result);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(gatewayOperator).authorizeConsumer(eq(gateway), eq("gw-consumer-1"), captor.capture());
        assertTrue(captor.getValue() instanceof ApisixRefConfig);
        assertEquals("route-1", ((ApisixRefConfig) captor.getValue()).getRouteId());
    }

    @Test
    void getGatewayConfigShouldIncludeApisixConfig() {
        Gateway gateway = new Gateway();
        gateway.setGatewayId("apisix-1");
        gateway.setGatewayType(GatewayType.APISIX);

        ApisixConfig apisixConfig = new ApisixConfig();
        apisixConfig.setAdminApiEndpoint("http://localhost:9180");
        apisixConfig.setAdminApiKey("test-key");
        gateway.setApisixConfig(apisixConfig);

        when(gatewayRepository.findByGatewayId("apisix-1")).thenReturn(Optional.of(gateway));

        GatewayConfig config = gatewayService.getGatewayConfig("apisix-1");
        assertNotNull(config.getApisixConfig());
        assertEquals("http://localhost:9180", config.getApisixConfig().getAdminApiEndpoint());
    }
}

