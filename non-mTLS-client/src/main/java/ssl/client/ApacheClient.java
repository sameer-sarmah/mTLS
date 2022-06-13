package ssl.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLSession;

import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import ssl.exception.CoreException;
import ssl.util.CertificateAnalyser;

@Component
public class ApacheClient {


	final static Logger logger = Logger.getLogger(ApacheClient.class);

	public HttpResponse request(final String url, final String method, Map<String, String> headers,
			Map<String, String> queryParams, final String jsonString) throws CoreException {

		try {
			RequestBuilder requestBuilder = RequestBuilder.create(method);
			if (headers != null) {
				headers.forEach((key, value) -> {
					requestBuilder.addHeader(key, value);
				});
			}
			if (queryParams != null) {
				queryParams.forEach((key, value) -> {
					NameValuePair pair = new BasicNameValuePair(key, value);
					requestBuilder.addParameters(pair);
				});
			}

			requestBuilder.setUri(url);

			if (method.equals(HttpPost.METHOD_NAME) || method.equals(HttpPut.METHOD_NAME)) {
				StringEntity input = new StringEntity(jsonString);
				input.setContentType("application/json");
				requestBuilder.setEntity(input);
			}
			  HttpResponseInterceptor certificateInterceptor = (httpResponse, context) -> {
		            ManagedHttpClientConnection routedConnection = (ManagedHttpClientConnection)context.getAttribute(HttpCoreContext.HTTP_CONNECTION);
		            SSLSession sslSession = routedConnection.getSSLSession();
		            if (sslSession != null) {
		            	X509Certificate[] certificates = (X509Certificate[]) sslSession.getPeerCertificates();
		                CertificateAnalyser.analyse(certificates);		             
		            }
		        };
			HttpUriRequest request = requestBuilder.build();
			CloseableHttpClient client = HttpClientBuilder.create()
											.addInterceptorLast(certificateInterceptor)
											.build();
			HttpResponse response = client.execute(request);

			return response;

		} catch (MalformedURLException e) {
			logger.error(e.getMessage());
			throw new CoreException(e.getMessage(), 500);

		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new CoreException(e.getMessage(), 500);
		} catch (Exception e) {
			throw new CoreException(e.getMessage(), 500);
		}
	}
	
}
