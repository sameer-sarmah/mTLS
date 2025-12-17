package northwind.client;

import northwind.exception.CoreException;
import northwind.http.api.IHttpClientBuilder;
import northwind.keystore.api.IKeystoreService;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.security.KeyStore;
import java.util.Map;

@Component
public class DefaultRestClient {
    @Value("${server.ssl.key-password}")
    private String keyPwd;

    @Qualifier("PkcsKeystoreService")
    @Autowired
    private IKeystoreService keyStoreUtil;

    @Autowired
    private IHttpClientBuilder clientBuilder;

    private static final String PKCS = "PKCS12";

    public String request(String url, HttpMethod method, Map<String, String> headers,
                          Map<String, String> queryParams, String jsonString) throws CoreException {
        try{
            KeyStore keystore = keyStoreUtil.readStore(PKCS);
            CloseableHttpClient httpClient = clientBuilder.buildHttpClient(keystore, keyPwd);
            ClientHttpRequestFactory httpRequestFactory = new BufferingClientHttpRequestFactory(
                    new HttpComponentsClientHttpRequestFactory(httpClient));
            RestClient customClient = RestClient.builder()
                    .requestFactory(new HttpComponentsClientHttpRequestFactory())
                    .baseUrl(url)
                    .build();
            RestClient.RequestBodyUriSpec requestSpec = customClient.method(method);
            if(headers != null){
                headers.forEach((key,value)->{
                    requestSpec.header(key,value);
                });
            }

            if(method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)) {
                return  requestSpec.body(jsonString).retrieve().body(String.class);
            } else{
               return requestSpec.retrieve().body(String.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
