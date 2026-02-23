package com.alibaba.apiopenplatform.service.gateway;

import com.alibaba.apiopenplatform.entity.Gateway;
import com.alibaba.apiopenplatform.service.gateway.client.ApisixClient;
import com.alibaba.apiopenplatform.service.gateway.client.GatewayClient;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import com.alibaba.apiopenplatform.support.consumer.ApisixAuthConfig;
import com.alibaba.apiopenplatform.support.consumer.ConsumerAuthConfig;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.product.ApisixRefConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApisixConsumerRestrictionAuthTest {

    @Mock
    private ApisixClient client;

    private ApisixOperator operator;
    private Gateway gateway;

    @BeforeEach
    void setUp() {
        operator = new ApisixOperator();

        gateway = new Gateway();
        gateway.setGatewayId("test-gateway-id");
        gateway.setGatewayType(GatewayType.APISIX);
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-key");
        gateway.setApisixConfig(config);

        Map<String, GatewayClient> clientCache = new ConcurrentHashMap<>();
        clientCache.put(gateway.getApisixConfig().buildUniqueKey(), client);
        ReflectionTestUtils.setField(operator, "clientCache", clientCache);
    }

    @Test
    void authorizeConsumerShouldWhitelistConsumerOnRoute() {
        ApisixRoute route = new ApisixRoute();
        route.setId("route-1");
        route.setUri("/mcp/filesystem/*");
        // use immutable maps to ensure operator copies before mutation
        route.setPlugins(Map.of("mcp-bridge", Map.of("command", "/bin/echo")));

        when(client.getRoute("route-1")).thenReturn(route);

        ApisixRefConfig refConfig = new ApisixRefConfig();
        refConfig.setRouteId("route-1");
        refConfig.setMcpServerName("filesystem");

        ConsumerAuthConfig authConfig = operator.authorizeConsumer(gateway, "consumer-1", refConfig);

        ArgumentCaptor<ApisixRoute> captor = ArgumentCaptor.forClass(ApisixRoute.class);
        verify(client).updateRoute(eq("route-1"), captor.capture());

        ApisixRoute updated = captor.getValue();
        assertNotNull(updated.getPlugins());
        assertTrue(updated.getPlugins().containsKey("key-auth"));
        assertTrue(updated.getPlugins().containsKey("consumer-restriction"));
        assertTrue(updated.getPlugins().containsKey("mcp-bridge"));

        Object restrictionObj = updated.getPlugins().get("consumer-restriction");
        assertInstanceOf(Map.class, restrictionObj);
        Map<?, ?> restriction = (Map<?, ?>) restrictionObj;
        assertEquals("consumer_name", restriction.get("type"));

        Object whitelistObj = restriction.get("whitelist");
        assertInstanceOf(List.class, whitelistObj);
        List<?> whitelist = (List<?>) whitelistObj;
        assertTrue(whitelist.contains("consumer-1"));

        assertNotNull(authConfig.getApisixAuthConfig());
        assertEquals("route-1", authConfig.getApisixAuthConfig().getRouteId());
    }

    @Test
    void revokeConsumerAuthorizationShouldRemovePluginWhenWhitelistEmpty() {
        ApisixRoute route = new ApisixRoute();
        route.setId("route-1");
        route.setUri("/api/*");
        route.setPlugins(Map.of(
                "key-auth", Map.of(),
                "consumer-restriction", Map.of(
                        "type", "consumer_name",
                        "whitelist", List.of("consumer-1")
                )
        ));

        when(client.getRoute("route-1")).thenReturn(route);

        ConsumerAuthConfig authConfig = ConsumerAuthConfig.builder()
                .apisixAuthConfig(ApisixAuthConfig.builder().routeId("route-1").build())
                .build();

        operator.revokeConsumerAuthorization(gateway, "consumer-1", authConfig);

        ArgumentCaptor<ApisixRoute> captor = ArgumentCaptor.forClass(ApisixRoute.class);
        verify(client).updateRoute(eq("route-1"), captor.capture());

        ApisixRoute updated = captor.getValue();
        assertNotNull(updated.getPlugins());
        assertTrue(updated.getPlugins().containsKey("key-auth"));
        assertFalse(updated.getPlugins().containsKey("consumer-restriction"));
    }

    @Test
    void revokeConsumerAuthorizationShouldKeepPluginWhenWhitelistNotEmpty() {
        ApisixRoute route = new ApisixRoute();
        route.setId("route-1");
        route.setUri("/api/*");
        route.setPlugins(Map.of(
                "key-auth", Map.of(),
                "consumer-restriction", Map.of(
                        "type", "consumer_name",
                        "whitelist", List.of("consumer-1", "consumer-2")
                )
        ));

        when(client.getRoute("route-1")).thenReturn(route);

        ConsumerAuthConfig authConfig = ConsumerAuthConfig.builder()
                .apisixAuthConfig(ApisixAuthConfig.builder().routeId("route-1").build())
                .build();

        operator.revokeConsumerAuthorization(gateway, "consumer-1", authConfig);

        ArgumentCaptor<ApisixRoute> captor = ArgumentCaptor.forClass(ApisixRoute.class);
        verify(client).updateRoute(eq("route-1"), captor.capture());

        ApisixRoute updated = captor.getValue();
        assertNotNull(updated.getPlugins());
        assertTrue(updated.getPlugins().containsKey("consumer-restriction"));

        Object restrictionObj = updated.getPlugins().get("consumer-restriction");
        assertInstanceOf(Map.class, restrictionObj);
        Map<?, ?> restriction = (Map<?, ?>) restrictionObj;

        Object whitelistObj = restriction.get("whitelist");
        assertInstanceOf(List.class, whitelistObj);
        List<?> whitelist = (List<?>) whitelistObj;
        assertFalse(whitelist.contains("consumer-1"));
        assertTrue(whitelist.contains("consumer-2"));
    }
}

