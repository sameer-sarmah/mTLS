package northwind.client;

import northwind.exception.CoreException;
import northwind.http.api.IHttpClientBuilder;
import northwind.keystore.api.IKeystoreService;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.KeyStore;
import java.util.Map;

@Component
public class DefaultRestTemplate {

    @Value("${server.ssl.key-password}")
    private String keyPwd;

    @Qualifier("PkcsKeystoreService")
    @Autowired
    private IKeystoreService keyStoreUtil;

    private static final String PKCS = "PKCS12";

    @Autowired
    private IHttpClientBuilder clientBuilder;

    public String request(String url, HttpMethod method, Map<String, String> headers,
                          Map<String, String> queryParams, String jsonString) throws CoreException {

        try {
            KeyStore keystore = keyStoreUtil.readStore(PKCS);
            CloseableHttpClient httpClient = clientBuilder.buildHttpClient(keystore, keyPwd);
            ClientHttpRequestFactory httpRequestFactory = new BufferingClientHttpRequestFactory(
                    new HttpComponentsClientHttpRequestFactory(httpClient));
            RestTemplateBuilder  restTemplateBuilder = new RestTemplateBuilder();
            RestTemplate restTemplate = restTemplateBuilder.build();
            restTemplate.setRequestFactory(httpRequestFactory);
            // Build URL with query parameters
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
            if (queryParams != null) {
                queryParams.forEach(uriBuilder::queryParam);
            }

            // Set up headers
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            if (jsonString != null) {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            }

            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonString, httpHeaders);

            // Execute request
            ResponseEntity<String> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    method,
                    requestEntity,
                    String.class
            );

            return response.getBody();


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
