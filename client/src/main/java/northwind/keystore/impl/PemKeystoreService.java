package northwind.keystore.impl;

import northwind.keystore.api.IKeystoreService;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component("PemKeystoreService")
public class PemKeystoreService implements IKeystoreService {

    @Value("${server.ssl.public-certs}")
    private Resource publicCerts;
    @Value("${server.ssl.private-key}")
    private Resource privateKey;

    @Value("${server.ssl.validate-cert-chain:false}")
    private boolean validateCertChain;

    private static final String PEM = "PEM";

    private static final String SHA256withRSA = "SHA256withRSA";

    @Override
    public KeyStore readStore(String keyStoreType) throws Exception {
        if(StringUtils.isBlank(keyStoreType) || !keyStoreType.equalsIgnoreCase(PEM)) {
            throw new IllegalArgumentException("Unsupported keystore type: " + keyStoreType);
        }
        // Creates an EMPTY keystore in memory
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        PrivateKey privateKey = loadPrivateKey();
        Certificate[] certChain = loadCertificates();

        validatePrivateKeyMatchesCertificate(privateKey, certChain[0]);

        Certificate[] chainToUse = new Certificate[] { certChain[0] };

        //char[] password = UUID.randomUUID().toString().toCharArray();
        char[] password = "password".toCharArray();
        /*
        * KeyStore contains one entry named "client" with private key and certificate chain.
        * */
        String alias = "client";
        keyStore.setKeyEntry(alias, privateKey, password, chainToUse);

        return keyStore;
    }

    /**
     * Load private key from PEM format using Bouncy Castle PEMParser
     */
    private PrivateKey loadPrivateKey() throws Exception {
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(privateKey.getInputStream()))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            if (object instanceof PEMKeyPair) {
                // Handle key pair (older format)
                return converter.getPrivateKey(((PEMKeyPair) object).getPrivateKeyInfo());
            } else if (object instanceof PrivateKeyInfo) {
                // Handle PKCS8 private key
                return converter.getPrivateKey((PrivateKeyInfo) object);
            } else {
                throw new IllegalArgumentException("Unsupported private key format: " +
                    (object != null ? object.getClass().getName() : "null"));
            }
        }
    }

    /**
     * Load certificates from PEM format using Bouncy Castle PEMParser
     */
    private Certificate[] loadCertificates() throws Exception {
        List<Certificate> certificates = new ArrayList<>();
        JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter();

        try (PEMParser pemParser = new PEMParser(new InputStreamReader(publicCerts.getInputStream()))) {
            Object object = pemParser.readObject();
            while (Objects.nonNull(object)) {
                if (object instanceof X509CertificateHolder certificateHolder) {
                    certificates.add(certificateConverter.getCertificate(certificateHolder));
                }
                object = pemParser.readObject();
            }
        }

        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("No certificates found in PEM file");
        }

        return certificates.toArray(new Certificate[0]);
    }

    /*
    * A private key can sign data. The corresponding public key (in the certificate) can verify that signature.
    * Only the matching public key can successfully verify a signature created by its private key
    * */
    private void validatePrivateKeyMatchesCertificate(PrivateKey privateKey, Certificate certificate) throws Exception {

        Signature signature = Signature.getInstance(SHA256withRSA);
        signature.initSign(privateKey);
        byte[] testData = "test".getBytes();
        signature.update(testData);
        byte[] signatureBytes = signature.sign();

        signature = Signature.getInstance(SHA256withRSA);
        signature.initVerify(certificate.getPublicKey());
        signature.update(testData);

        if (signature.verify(signatureBytes)) {
            System.out.println("✓ Private key matches the certificate");
        } else {
            throw new IllegalArgumentException("Private key does NOT match the first certificate in the chain!");
        }
    }

    /**
     * Validate that the certificate chain is properly formed.
     * The chain should be ordered: entity certificate, intermediate CAs (if any), root CA.
     * Each certificate should be signed by the next one in the chain.
     */
    private void validateCertificateChain(Certificate[] certChain) throws Exception {
        if (certChain == null || certChain.length == 0) {
            throw new IllegalArgumentException("Certificate chain is empty");
        }

        System.out.println("Validating certificate chain with " + certChain.length + " certificate(s)");

        // Special case: single certificate (could be self-signed)
        if (certChain.length == 1) {
            try {
                certChain[0].verify(certChain[0].getPublicKey());
                System.out.println("Single self-signed certificate detected and validated successfully");
                return; // Self-signed certificate is valid
            } catch (Exception e) {
                System.out.println("Warning: Single certificate is not self-signed. " +
                    "This might be okay if the CA is in the truststore.");
                // Don't fail - single cert might be signed by a CA in truststore
                return;
            }
        }

        // Multiple certificates: validate the chain
        for (int i = 0; i < certChain.length - 1; i++) {
            try {
                certChain[i].verify(certChain[i + 1].getPublicKey());
                System.out.println("Certificate at index " + i + " is signed by certificate at index " + (i + 1));
            } catch (Exception e) {
                System.err.println("Warning: Certificate at index " + i +
                    " is NOT signed by certificate at index " + (i + 1));
                System.err.println("Error details: " + e.getMessage());

                // Try reverse order - maybe certificates are in wrong order
                try {
                    certChain[i + 1].verify(certChain[i].getPublicKey());
                    System.err.println("ERROR: Certificates appear to be in REVERSE order!");
                    System.err.println("Please reverse the order of certificates in your PEM file.");
                    System.err.println("Current order should be: Certificate[" + (i+1) + "] -> Certificate[" + i + "]");
                    throw new IllegalArgumentException(
                        "Certificate chain is in wrong order. Certificate at index " + (i + 1) +
                        " should come before certificate at index " + i, e);
                } catch (Exception e2) {
                    // Not in reverse order either - certificates may not form a chain
                    System.err.println("Certificates do not form a valid chain.");
                    System.err.println("This may be okay if:");
                    System.err.println("  1. You only have the entity certificate (intermediate/root CAs are in truststore)");
                    System.err.println("  2. The certificate will be validated during SSL handshake");
                    System.err.println("\nTo bypass this validation, set: server.ssl.validate-cert-chain=false");
                    throw new IllegalArgumentException(
                        "Certificate chain is not valid: Certificate at index " + i +
                        " is not signed by certificate at index " + (i + 1) +
                        ". Error: " + e.getMessage(), e);
                }
            }
        }

        // Validate the last certificate (should be root CA - self-signed)
        try {
            certChain[certChain.length - 1].verify(certChain[certChain.length - 1].getPublicKey());
            System.out.println("Root certificate (index " + (certChain.length - 1) + ") is self-signed");
        } catch (Exception e) {
            System.out.println("Warning: Last certificate in chain is not self-signed. " +
                "Make sure it's signed by a trusted CA in the truststore.");
        }

        System.out.println("Certificate chain validation completed successfully");
    }


}
