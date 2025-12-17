package northwind.keystore.api;

import java.security.KeyStore;

public interface IKeystoreService {
    public KeyStore readStore(String keyStoreType) throws Exception;
}
