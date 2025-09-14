package ssl.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ssl.client.ApacheClient;
import ssl.exception.CoreException;


@RestController
public class ProductController {
	
	@Value("${product.service.url}")
    private String  productServiceUrl;
	
	@Value("${product.service.host}")
    private String  productServiceHost;
	
	@Autowired
	private ApacheClient client;
	
    @RequestMapping( value = "/products",method = RequestMethod.GET)
    public String getProducts( HttpServletRequest request,HttpServletResponse response) {

    	try {
    		String responseFromProductService =client.request(productServiceUrl, HttpGet.METHOD_NAME, Map.of(), Map.of(), null);
			return responseFromProductService;
    		
		}  catch (CoreException e) {
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		}

        return "{}";
    }


}
