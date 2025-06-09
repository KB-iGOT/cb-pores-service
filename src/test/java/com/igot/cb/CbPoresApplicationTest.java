package com.igot.cb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;


class CbPoresApplicationTest {

    private final CbPoresApplication app = new CbPoresApplication();

    @Test
    void restTemplateBean_ShouldNotBeNull() {
        RestTemplate restTemplate = app.restTemplate();

        assertNotNull(restTemplate, "RestTemplate should not be null");
        ClientHttpRequestFactory factory = restTemplate.getRequestFactory();
        assertNotNull(factory, "ClientHttpRequestFactory should not be null");
        assertTrue(factory.toString().contains("HttpComponentsClientHttpRequestFactory"),
                "Request factory should be an instance of HttpComponentsClientHttpRequestFactory");
    }
}
