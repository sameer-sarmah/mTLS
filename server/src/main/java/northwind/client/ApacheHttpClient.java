package northwind.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import northwind.exception.CoreException;
import northwind.util.HttpMethod;


public class ApacheHttpClient{
    final static Logger logger = Logger.getLogger(ApacheHttpClient.class);
    public String request(final String url,final HttpMethod method,Map<String,String> headers ,
                          Map<String,String> queryParams,final String jsonString) throws CoreException{

        try {
            RequestBuilder requestBuilder= RequestBuilder.create(method.toString());
            if(headers != null){
                headers.forEach((key,value)->{
                    requestBuilder.addHeader(key,value);
                });
            }
            if(queryParams != null) {
                queryParams.forEach((key, value) -> {
                    NameValuePair pair = new BasicNameValuePair(key, value);
                    requestBuilder.addParameters(pair);
                });
            }


            requestBuilder.setUri(url);


            if(method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)){
                StringEntity input = new StringEntity(jsonString);
                input.setContentType("application/json");
                requestBuilder.setEntity(input);
            }

            HttpUriRequest request=requestBuilder.build();
            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpResponse response = client.execute(request);



            return  EntityUtils.toString(response.getEntity());


        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
            throw new CoreException(e.getMessage(),500);

        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new CoreException(e.getMessage(),500);
        }

    }
}
