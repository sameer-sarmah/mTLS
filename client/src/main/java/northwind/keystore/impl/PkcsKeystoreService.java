package northwind.keystore.impl;

import northwind.keystore.api.IKeystoreService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyStore;

@Component(value = "PkcsKeystoreService")
public class PkcsKeystoreService implements IKeystoreService {

    @Value("${key-store-file}")
    private String keystoreFile;
    @Value("${server.ssl.key-store-password}")
    private String keystorePwd;
    @Value("${server.ssl.key-password}")
    private String keyPwd;
    @Value("${server.ssl.key-store-type}")
    private String keyStoreType;

    @Override
    public KeyStore readStore(String keyStoreType) throws Exception {

        if(StringUtils.isBlank(keyStoreType) || !keyStoreType.equalsIgnoreCase(this.keyStoreType)) {
            throw new IllegalArgumentException("Unsupported keystore type: " + keyStoreType);

        }
        try (InputStream keyStoreStream = this.getClass().getClassLoader().getSystemResourceAsStream(keystoreFile)) {
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(keyStoreStream, keystorePwd.toCharArray());
            return keyStore;
        }
    }
}
