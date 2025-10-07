package northwind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/*
Swagger API 
http://localhost:8080/v2/api-docs

Swagger UI
http://localhost:8080/swagger-ui.html
 * */

@Configuration
@ComponentScan(basePackages= {"northwind"})
public class AppConfig {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

}
