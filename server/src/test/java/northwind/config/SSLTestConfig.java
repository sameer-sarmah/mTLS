package northwind.config;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@TestConfiguration
public class SSLTestConfig {

	public KeyStore readStore() throws Exception {
		String keyStoreType = "PKCS12";
		String keystorePwd = "password";
		String keystoreFile = "client.p12";
		try (InputStream keyStoreStream = this.getClass().getClassLoader().getSystemResourceAsStream(keystoreFile)) {
			KeyStore keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(keyStoreStream, keystorePwd.toCharArray());
			return keyStore;
		}
	}

    @Bean
    @Primary
    public TestRestTemplate testRestTemplate() throws Exception {
        SSLContext sslContext = createSSLContext();
        
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder
                        		.create()
                                .setSslContext(sslContext)
                                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .build())
                        .build())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = 
                new HttpComponentsClientHttpRequestFactory(httpClient);

        TestRestTemplate restTemplate = new TestRestTemplate(new RestTemplateBuilder()
                .requestFactory(() -> requestFactory));
        return restTemplate;
    }

    private SSLContext createSSLContext() throws Exception {
        
    	KeyStore keyStore = readStore();

        return SSLContextBuilder.create()
                .loadKeyMaterial(keyStore, "password".toCharArray())
                .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
                .build();
    }
}