package northwind.http.api;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.security.KeyStore;

public interface IHttpClientBuilder {
    CloseableHttpClient buildHttpClient(KeyStore keyStore, String keyStorePwd);
}
