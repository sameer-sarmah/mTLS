package northwind.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import northwind.client.ApacheClient;
import northwind.model.Product;




@RestController
public class ProductController {
	
	@Value("${product.service.url}")
    private String  productServiceUrl;
	
	@Autowired
	private ApacheClient client;
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
	
    @RequestMapping( value = "/products",method = RequestMethod.GET)
    public List<Product> getProducts( HttpServletRequest request,HttpServletResponse response) {

    	try {
    		String responseFromProductService =client.request(productServiceUrl, HttpMethod.GET, Map.of(), Map.of(), null);
			List<Product> products = objectMapper.readValue(responseFromProductService, new TypeReference<List<Product>>() {});
			logger.info("Retrieved {} products from external service", products.size());
			return products;
    		
		}  catch (Exception e) {
			logger.error("Failed to retrieve products from external service: {}", e.getMessage(), e);
			return List.of();
		} 
    }


}
