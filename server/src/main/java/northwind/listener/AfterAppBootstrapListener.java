package northwind.listener;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import northwind.util.CertificateAnalyser;
import northwind.util.KeyStoreUtil;
import northwind.util.TrustStoreUtil;

@Component
public class AfterAppBootstrapListener implements ApplicationListener<ApplicationEvent> {
	
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
	
	@Autowired
	private TrustStoreUtil trustStoreUtil;
	
	  @Override
	  public void onApplicationEvent(ApplicationEvent event) {
	     if(event instanceof ApplicationStartedEvent) {
				try {
					KeyStore keystore = keyStoreUtil.readStore();
					KeyStore trustStore = trustStoreUtil.readStore();
					List<String> publicKeys = new ArrayList<String>();
					publicKeys.add("sfsf-client");
					publicKeys.add("client");
					publicKeys.add("server");
					analysePublicCertificates(trustStore,publicKeys);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

	     }
	  }
	  
	  
		public  void analysePrivateCertificates(KeyStore keyStore,String privateKeyName) {
			try {
				if(Objects.nonNull(privateKeyName)) {
					Key privateKey = keyStore.getKey(privateKeyName, keystorePwd.toCharArray());
					System.out.println(String.format("algorithm : %s,format : %s",privateKey.getAlgorithm(),privateKey.getFormat()));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private  void analysePublicCertificates(KeyStore keyStore,List<String> publicKeys) {
			try {
				System.out.println(String.format("Size of keystore: %s, type of keystore: %s ",keyStore.size(),keyStore.getType()));
				publicKeys.stream().forEach((publicKey) ->{
					try {
						Certificate clientCertificate = keyStore.getCertificate(publicKey);
						if(clientCertificate instanceof X509Certificate) {
							CertificateAnalyser.analyse((X509Certificate)clientCertificate);
						}				
					} catch (KeyStoreException e) {
						e.printStackTrace();
					}	
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
}
