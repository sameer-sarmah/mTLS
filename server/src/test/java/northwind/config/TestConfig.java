package northwind.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import northwind.client.ApacheHttpClient;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public ApacheHttpClient mockApacheHttpClient() {
        return Mockito.mock(ApacheHttpClient.class);
    }
}