package northwind.controller;

import northwind.app.ServerApplication;
import northwind.client.ApacheHttpClient;
import northwind.exception.CoreException;
import northwind.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(
    classes = {ServerApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureTestRestTemplate
public class ProductsControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private ApacheHttpClient mockApacheHttpClient;

    @Test
    public void testGetProducts_Success() throws CoreException, IOException {

        ClassPathResource resource = new ClassPathResource("products.json");
        String mockResponse = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        when(mockApacheHttpClient.request(
                eq("https://services.odata.org/Northwind/Northwind.svc/Products"),
                eq(HttpMethod.GET),
                any(Map.class),
                any(Map.class),
                isNull()
        )).thenReturn(mockResponse);

        String url = "https://localhost:" + port + "/products";
        
        ResponseEntity<List<Product>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Product>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());

        Product chai = response.getBody().get(0);
        assertEquals("1", chai.ProductID());
        assertEquals("Chai", chai.ProductName());


        Product chang = response.getBody().get(1);
        assertEquals("2", chang.ProductID());
        assertEquals("Chang", chang.ProductName());
    }


}