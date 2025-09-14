package ssl.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLSession;


import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.log4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import ssl.exception.CoreException;
import ssl.util.CertificateAnalyser;

@Component
public class ApacheClient {


	final static Logger logger = Logger.getLogger(ApacheClient.class);

	public String request(final String url, final String method, Map<String, String> headers,
								Map<String, String> queryParams, final String jsonString) throws CoreException {

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

			requestBuilder.setUri(url);

			if(method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)){
				StringEntity input = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
				requestBuilder.setEntity(input);
			}

			HttpResponseInterceptor certificateInterceptor = (HttpResponse httpResponse, EntityDetails entityDetails, HttpContext context) -> {
				// Try to get SSL session directly from context first
				SSLSession sslSession = (SSLSession) context.getAttribute(HttpCoreContext.SSL_SESSION);
				if (sslSession != null) {
					try {
						X509Certificate[] certificates = (X509Certificate[]) sslSession.getPeerCertificates();
						CertificateAnalyser.analyse(certificates);
					} catch (Exception e) {
						logger.warn("Failed to analyze certificates from SSL session: " + e.getMessage());
					}
				} else {
					logger.info("No SSL session found in context");
				}
			};

			CloseableHttpClient httpClient = HttpClientBuilder.create()
					.addResponseInterceptorLast(certificateInterceptor)
					.build();


			ClassicHttpRequest request=requestBuilder.build();
			return  getResponse(httpClient,request);

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

	private String getResponse(HttpClient httpClient, ClassicHttpRequest request) throws IOException {
		HttpClientResponseHandler<String> responseHandler = (ClassicHttpResponse response) -> {
			InputStream inputStream = response.getEntity().getContent();
			String responseStr = IOUtils.toString(inputStream, Charset.defaultCharset());
			return responseStr;
		};
		return httpClient.execute(request,responseHandler);
	}
}
