package northwind.client;

import northwind.exception.CoreException;
import northwind.util.HttpMethod;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.Map;

@Component
public class ApacheHttpClient{
    final static Logger logger = LoggerFactory.getLogger(ApacheHttpClient.class);

    public String request(final String url,final HttpMethod method,Map<String,String> headers ,
                          Map<String,String> queryParams,final String jsonString) throws CoreException{

        try {
            ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.create(method.toString());
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

            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            requestBuilder.setUri(url);


            if(method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)){
                StringEntity input = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
                requestBuilder.setEntity(input);
            }

            ClassicHttpRequest request=requestBuilder.build();
            CloseableHttpClient client = HttpClientBuilder.create().build();
            return  getResponse(httpClient,request);


        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
            throw new CoreException(e.getMessage(),500);

        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new CoreException(e.getMessage(),500);
        }

    }


    private String getResponse(HttpClient httpClient,ClassicHttpRequest request) throws IOException {
        HttpClientResponseHandler<String> responseHandler = (ClassicHttpResponse response) -> {
            InputStream inputStream = response.getEntity().getContent();
            String responseStr = IOUtils.toString(inputStream, Charset.defaultCharset());
            return responseStr;
        };
        return httpClient.execute(request,responseHandler);
    }
}
