package northwind.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import northwind.app.ServerApplication;
import northwind.model.Product;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = {ServerApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
    "northwind.service.url=http://localhost:8089"
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
        // Start WireMock server on port 8089 using WireMock 3.x API
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8089));
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
        // Setup WireMock stub for successful response
        ClassPathResource resource = new ClassPathResource("products.json");
        String mockResponse = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        wireMockServer.stubFor(get(urlPathEqualTo("/Products"))
                .withQueryParam("$format", equalTo("json"))
                .withQueryParam("$filter", equalTo("CategoryID eq 1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // Make request to our controller
        String url = "https://localhost:" + port + "/products";
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
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/Products"))
                .withQueryParam("$format", equalTo("json"))
                .withQueryParam("$filter", equalTo("CategoryID eq 1")));
    }

}