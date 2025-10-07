package northwind.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import northwind.client.ApacheHttpClient;
import northwind.model.Product;


@RestController
public class ProductsController {
	
	private static final Logger logger = LoggerFactory.getLogger(ProductsController.class);
	
	@Autowired
	private ApacheHttpClient httpClient;
	
	private final ObjectMapper objectMapper = new ObjectMapper();
    
    @RequestMapping( value = "/products",method = RequestMethod.GET)
	public List<Product> getProducts(HttpServletRequest request) {
		String url = "https://services.odata.org/Northwind/Northwind.svc/Products";
		String jsonResponse = "";
		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("$format", "json");
		queryParams.put("$filter", "CategoryID eq 1");
		try {
			jsonResponse = httpClient.request(url, HttpMethod.GET, Map.of(), queryParams,null);
			JsonNode response = objectMapper.readTree(jsonResponse);
			JsonNode valueNode = response.get("value");
			List<Product> products = objectMapper.readValue(valueNode.toString(), new TypeReference<List<Product>>() {});
			logger.info("Retrieved {} products from external service", products.size());
			return products;
		} catch (Exception e) {
			logger.error("Failed to retrieve products from external service: {}", e.getMessage(), e);
			return List.of();
		} 
	}
}