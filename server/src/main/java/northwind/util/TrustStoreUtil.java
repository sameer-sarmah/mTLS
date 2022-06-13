package northwind.util;

import java.io.InputStream;
import java.security.KeyStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TrustStoreUtil {
	@Value("${trust-store-file}")
	private String trustStoreFile;
	@Value("${server.ssl.trust-store-password}")
	private String trustStorePwd;
	@Value("${server.ssl.trust-store-password}")
	private String keyPwd;
	@Value("${server.ssl.trust-store-type}")
	private String trustStoreType;

	public KeyStore readStore() throws Exception {
		try (InputStream trustStoreStream = this.getClass().getClassLoader().getSystemResourceAsStream(trustStoreFile)) {
			KeyStore keyStore = KeyStore.getInstance(trustStoreType);
			keyStore.load(trustStoreStream, trustStorePwd.toCharArray());
			return keyStore;
		}
	}
}
