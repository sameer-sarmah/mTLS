package ssl.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

import ssl.config.AppConfig;


@SpringBootApplication
@Import({AppConfig.class})
public class NonTLSClientApplication  extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    	return application.sources(NonTLSClientApplication.class);
    }
	
	public static void main(String[] args) {
		SpringApplication.run(NonTLSClientApplication.class, args);
		System.err.println("##########Non MTLS ClientApplication########");
		
	}

}
