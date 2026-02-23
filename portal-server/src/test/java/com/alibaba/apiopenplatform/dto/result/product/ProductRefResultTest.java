package com.alibaba.apiopenplatform.dto.result.product;

import com.alibaba.apiopenplatform.entity.ProductRef;
import com.alibaba.apiopenplatform.support.product.ApisixRefConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductRefResultTest {

    @Test
    void shouldIncludeApisixRefConfig() {
        ProductRef ref = new ProductRef();
        ref.setProductId("product-1");

        ApisixRefConfig apisixRefConfig = new ApisixRefConfig();
        apisixRefConfig.setRouteId("route-1");
        apisixRefConfig.setMcpServerName("mcp-1");
        ref.setApisixRefConfig(apisixRefConfig);

        ProductRefResult result = new ProductRefResult().convertFrom(ref);

        assertNotNull(result.getApisixRefConfig());
        assertEquals("route-1", result.getApisixRefConfig().getRouteId());
        assertEquals("mcp-1", result.getApisixRefConfig().getMcpServerName());
    }
}

