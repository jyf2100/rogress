package com.alibaba.apiopenplatform.service.gateway.client;

import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApisixClientConsumerExistsTest {

    @Test
    void consumerExistsShouldReturnFalseOn404() {
        ApisixClient client = spy(new ApisixClient(testConfig()));

        doThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                null,
                null,
                null
        )).when(client).getConsumer("missing");

        assertFalse(client.consumerExists("missing"));
    }

    @Test
    void consumerExistsShouldPropagateNon404HttpErrors() {
        ApisixClient client = spy(new ApisixClient(testConfig()));

        doThrow(HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                null,
                null,
                null
        )).when(client).getConsumer("any");

        assertThrows(HttpClientErrorException.class, () -> client.consumerExists("any"));
    }

    private static ApisixConfig testConfig() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-key");
        return config;
    }
}

