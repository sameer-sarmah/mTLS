package ssl.client;

import java.io.IOException;
import java.net.MalformedURLException;
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

import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ssl.exception.CoreException;
import ssl.util.CertificateAnalyser;
import ssl.util.KeyStoreUtil;

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

	public HttpResponse request(final String url, final String method, Map<String, String> headers,
			Map<String, String> queryParams, final String jsonString) throws CoreException {

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
					.register("https", sslsf).register("http", new PlainConnectionSocketFactory()).build();
			BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(
					socketFactoryRegistry);
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
											.setConnectionManager(connectionManager)
											.setSSLSocketFactory(sslsf)
											.setSSLContext(sslContext)
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
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new CoreException(e.getMessage(), 500);
		} catch (Exception e) {
			throw new CoreException(e.getMessage(), 500);
		}
	}
	
	public static void analyseKeystore(KeyStore keyStore,List<String> publicKeys,String privateKeyName) {
		try {
			System.out.println(String.format("Size of keystore: %s, type of keystore: %s ",keyStore.size(),keyStore.getType()));
			publicKeys.stream().forEach((publicKey) ->{
				try {
					Certificate clientCertificate = keyStore.getCertificate(publicKey);
					analyseCertificate(clientCertificate);
				} catch (KeyStoreException e) {
					e.printStackTrace();
				}	
			});
			Key privateKey = keyStore.getKey(privateKeyName, "password".toCharArray());
			System.out.println(String.format("algorithm : %s,format : %s",privateKey.getAlgorithm(),privateKey.getFormat()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void analyseCertificate(Certificate certificate) {
		PublicKey serverPublicKey = certificate.getPublicKey();
		System.out.println(String.format("algorithm : %s,format : %s",serverPublicKey.getAlgorithm(),serverPublicKey.getFormat()));
		try {
			certificate.verify(serverPublicKey);
		} catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException | NoSuchProviderException
				| SignatureException e) {
			e.printStackTrace();
		}
	}
}
