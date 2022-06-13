package northwind.util;

import java.io.InputStream;
import java.security.KeyStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KeyStoreUtil {
	@Value("${key-store-file}")
	private String keystoreFile;
	@Value("${server.ssl.key-store-password}")
	private String keystorePwd;
	@Value("${server.ssl.key-password}")
	private String keyPwd;
	@Value("${server.ssl.key-store-type}")
	private String keyStoreType;

	public KeyStore readStore() throws Exception {
		try (InputStream keyStoreStream = this.getClass().getClassLoader().getSystemResourceAsStream(keystoreFile)) {
			KeyStore keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(keyStoreStream, keystorePwd.toCharArray());
			return keyStore;
		}
	}
}
