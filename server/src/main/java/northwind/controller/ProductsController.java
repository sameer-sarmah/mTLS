package northwind.controller;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import northwind.client.ApacheHttpClient;
import northwind.exception.CoreException;
import northwind.model.Product;
import northwind.util.HttpMethod;


@RestController
public class ProductsController {
    
    @RequestMapping( value = "/products",method = RequestMethod.GET)
	public List<Product> getProducts(HttpServletRequest request) {
		String url = "https://services.odata.org/Northwind/Northwind.svc/Products";
		String jsonResponse = "";
		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("$format", "json");
		queryParams.put("$filter", "CategoryID eq 1");
		try {
			ApacheHttpClient httpClient = new ApacheHttpClient();
			Type typeToken = new TypeToken<List<Product>>() {
			}.getType();
			jsonResponse = httpClient.request(url, HttpMethod.GET, Collections.<String, String>emptyMap(), queryParams,
					null);
			JsonObject response = new Gson().fromJson(jsonResponse, JsonObject.class);
			jsonResponse = response.get("value").toString();
			return new Gson().fromJson(jsonResponse, typeToken);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	
}
