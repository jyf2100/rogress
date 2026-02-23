package com.alibaba.apiopenplatform.dto.result.mcp;

import com.alibaba.apiopenplatform.dto.result.common.DomainResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MCPConfigResultTest {

    @Test
    void shouldReplaceHigressGatewayIpPlaceholder() {
        MCPConfigResult config = new MCPConfigResult();
        MCPConfigResult.MCPServerConfig serverConfig = new MCPConfigResult.MCPServerConfig();
        serverConfig.setDomains(List.of(DomainResult.builder().domain("<higress-gateway-ip>").protocol("http").build()));
        config.setMcpServerConfig(serverConfig);

        config.convertDomainToGatewayIp(List.of("1.2.3.4"));

        assertEquals("1.2.3.4", config.getMcpServerConfig().getDomains().get(0).getDomain());
    }

    @Test
    void shouldReplaceApisixGatewayIpPlaceholder() {
        MCPConfigResult config = new MCPConfigResult();
        MCPConfigResult.MCPServerConfig serverConfig = new MCPConfigResult.MCPServerConfig();
        serverConfig.setDomains(List.of(DomainResult.builder().domain("<apisix-gateway-ip>").protocol("http").build()));
        config.setMcpServerConfig(serverConfig);

        config.convertDomainToGatewayIp(List.of("5.6.7.8"));

        assertEquals("5.6.7.8", config.getMcpServerConfig().getDomains().get(0).getDomain());
    }
}

