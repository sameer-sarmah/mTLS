package northwind.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import northwind.exception.CoreException;
import northwind.util.CertificateAnalyser;
import northwind.util.KeyStoreUtil;
@Component
public class ApacheClient {

	@Value("${server.ssl.key-store}")
	private String keystoreFile;
	@Value("${server.ssl.key-store-password}")
	private String keystorePwd;
	@Value("${server.ssl.key-password}")
	private String keyPwd;
	@Value("${server.ssl.key-store-type}")
	private String keyStoreType;
	@Autowired
	private KeyStoreUtil keyStoreUtil;

	final static Logger logger = Logger.getLogger(ApacheClient.class);

	public String request( String url, HttpMethod method, Map<String, String> headers,
								Map<String, String> queryParams, String jsonString) throws CoreException {

		try {
			KeyStore keystore = keyStoreUtil.readStore();
			List<String> publicKeys = new ArrayList<String>();
			publicKeys.add("client");
			publicKeys.add("server");
			analyseKeystore(keystore,publicKeys,"client");
			SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(keystore, keyPwd.toCharArray())
					.loadTrustMaterial(keystore, new TrustSelfSignedStrategy()).build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
					NoopHostnameVerifier.INSTANCE);

			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("https", sslsf)
					.register("http", new PlainConnectionSocketFactory())
					.build();

			var connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry);

			  HttpResponseInterceptor certificateInterceptor = (HttpResponse httpResponse,EntityDetails entityDetails, HttpContext context) -> {
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

				CloseableHttpClient httpClient = HttpClientBuilder.create()
						.setConnectionManager(connectionManager)
						.addResponseInterceptorLast(certificateInterceptor)
						.build();
				requestBuilder.setUri(url);


				if(method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)){
					StringEntity input = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
					requestBuilder.setEntity(input);
				}

				ClassicHttpRequest request=requestBuilder.build();
				return  getResponse(httpClient,request);

		} catch (MalformedURLException e) {
			logger.error(e.getMessage());
			throw new CoreException(e.getMessage(), 500);

		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new CoreException(e.getMessage(), 500);
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
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

	public static void analyseKeystore(KeyStore keyStore,List<String> publicKeys,String privateKeyName) {
		try {
			logger.info(String.format("Size of keystore: %s, type of keystore: %s ",keyStore.size(),keyStore.getType()));
			publicKeys.stream().forEach((publicKey) ->{
				try {
					Certificate clientCertificate = keyStore.getCertificate(publicKey);
					analyseCertificate(clientCertificate);
				} catch (KeyStoreException e) {
					e.printStackTrace();
				}	
			});
			Key privateKey = keyStore.getKey(privateKeyName, "password".toCharArray());
			logger.info(String.format("algorithm : %s,format : %s",privateKey.getAlgorithm(),privateKey.getFormat()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void analyseCertificate(Certificate certificate) {
		PublicKey serverPublicKey = certificate.getPublicKey();
		logger.info(String.format("algorithm : %s,format : %s",serverPublicKey.getAlgorithm(),serverPublicKey.getFormat()));
		try {
			certificate.verify(serverPublicKey);
		} catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException | NoSuchProviderException
				| SignatureException e) {
			e.printStackTrace();
		}
	}
}
