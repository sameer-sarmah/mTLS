package northwind.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import northwind.app.ClientApplication;
import northwind.model.Product;

@SpringBootTest(
    classes = {ClientApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
    "product.service.url=https://localhost:8089/products"
})
@ActiveProfiles("test")
public class ProductsControllerIntegrationTestWithWireMock {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private WireMockServer wireMockServer;

    @BeforeEach
    public void setup() {
    	WireMockConfiguration config =WireMockConfiguration.wireMockConfig()
        		.keystorePassword("password")
        		.keystoreType("PKCS12")
        		.keystorePath("src/test/resources/server.p12")
        		.trustStorePath("src/test/resources/server-truststore.p12")
        		.trustStorePassword("password")
        		.trustStoreType("PKCS12")
        		.httpsPort(8089)
        		.needClientAuth(true);
        // Start WireMock server on port 8089 using WireMock 3.x API
        wireMockServer = new WireMockServer(config);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    public void testGetProducts_Success() throws IOException {
 // This is the correct method
    	
        // Setup WireMock stub for successful response
        ClassPathResource resource = new ClassPathResource("products.json");
        String mockResponse = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        wireMockServer.stubFor(get(urlPathEqualTo("/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // Make request to our controller
        String url = "http://localhost:" + port + "/products";
        ResponseEntity<List<Product>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Product>>() {}
        );

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());

        Product chai = response.getBody().get(0);
        assertEquals("1", chai.ProductID());
        assertEquals("Chai", chai.ProductName());
        assertEquals("1", chai.CategoryID());

        Product chang = response.getBody().get(1);
        assertEquals("2", chang.ProductID());
        assertEquals("Chang", chang.ProductName());
        assertEquals("1", chang.CategoryID());

        // Verify that WireMock received the expected request
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/products")));
    }

}