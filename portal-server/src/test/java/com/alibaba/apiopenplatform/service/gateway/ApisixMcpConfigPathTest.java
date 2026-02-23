package com.alibaba.apiopenplatform.service.gateway;

import cn.hutool.json.JSONUtil;
import com.alibaba.apiopenplatform.dto.result.mcp.MCPConfigResult;
import com.alibaba.apiopenplatform.entity.Gateway;
import com.alibaba.apiopenplatform.service.gateway.client.ApisixClient;
import com.alibaba.apiopenplatform.service.gateway.client.GatewayClient;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.product.ApisixRefConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApisixMcpConfigPathTest {

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
    void fetchMcpConfigShouldNormalizeWildcardPath() {
        ApisixRoute route = new ApisixRoute();
        route.setId("route-1");
        route.setUri("/mcp/filesystem/*");
        route.setPlugins(Map.of("mcp-bridge", Map.of("command", "/bin/echo")));

        when(client.getRoute("route-1")).thenReturn(route);

        ApisixRefConfig refConfig = new ApisixRefConfig();
        refConfig.setRouteId("route-1");
        refConfig.setMcpServerName("filesystem");

        String json = operator.fetchMcpConfig(gateway, refConfig);
        MCPConfigResult parsed = JSONUtil.toBean(json, MCPConfigResult.class);

        assertNotNull(parsed.getMcpServerConfig());
        assertEquals("/mcp/filesystem", parsed.getMcpServerConfig().getPath());
    }
}

