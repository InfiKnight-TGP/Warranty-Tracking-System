package com.vault.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:soap-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "vault.scheduler.cron=0 0 0 1 1 *"
})
class WarrantyStatusEndpointTest {
    @Autowired private WarrantyStatusEndpoint endpoint;
    @Autowired private TestRestTemplate http;

    @Test
    void respondsWithDataFromAnActualSeededPurchase() {
        GetWarrantyStatusResponse response = endpoint.getWarrantyStatus(new GetWarrantyStatusRequest("WR-1001"));
        assertEquals("Refrigerator", response.getProductName());
        assertEquals("Whirlpool", response.getBrand());
        assertEquals("ACTIVE", response.getStatus());
        assertTrue(response.getDaysLeft() > 30);
    }

    @Test
    void publishesWsdlAndServesSoapRequestsOverHttp() {
        var wsdl = http.getForEntity("/ws/warranty.wsdl", String.class);
        assertEquals(200, wsdl.getStatusCode().value());
        assertTrue(wsdl.getBody().contains("WarrantyPort"));

        String xml = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:w="http://vault.com/ws/warranty">
                  <soap:Header/>
                  <soap:Body><w:GetWarrantyStatusRequest><w:serialNumber>WR-1001</w:serialNumber></w:GetWarrantyStatusRequest></soap:Body>
                </soap:Envelope>
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        var response = http.postForEntity("/ws", new HttpEntity<>(xml, headers), String.class);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("Refrigerator"));
        assertTrue(response.getBody().contains("Whirlpool"));
    }
}
