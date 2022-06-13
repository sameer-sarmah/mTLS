package ssl.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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
    public void getProducts( HttpServletRequest request,HttpServletResponse response) {

    	OutputStream servletOutputStream = null;
    	try {
    		servletOutputStream = response.getOutputStream();
    		HttpResponse responseFromProductService =client.request(productServiceUrl, HttpGet.METHOD_NAME, new HashMap<String,String>(), new HashMap<String,String>(), null);
    		HttpEntity responseEntity = responseFromProductService.getEntity();		
    		IOUtils.copy(responseEntity.getContent(), servletOutputStream);	
    		
		}  catch (CoreException e) {
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	finally {
    		if(servletOutputStream != null && !response.isCommitted()) {
    			try {
					servletOutputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}

    	
    }
    
    @RequestMapping( value = "/connectionTest",method = RequestMethod.GET)
    public void connectionTest( HttpServletRequest request,HttpServletResponse response) {

    	OutputStream servletOutputStream = null;
    	try {
    		servletOutputStream = response.getOutputStream();
    		HttpResponse responseFromProductService =client.request(productServiceHost, HttpGet.METHOD_NAME, new HashMap<String,String>(), new HashMap<String,String>(), null);
    		ByteArrayInputStream bis = new ByteArrayInputStream(responseFromProductService.getStatusLine().toString().getBytes());
    		IOUtils.copy(bis, servletOutputStream);	
		}  catch (CoreException e) {
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	finally {
    		if(servletOutputStream != null && !response.isCommitted()) {
    			try {
					servletOutputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}

    	
    }
	

}
