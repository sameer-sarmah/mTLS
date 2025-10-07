package northwind.util;

import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.stream.Stream;

public class CertificateAnalyser {
  
	public static void analyse(X509Certificate[] certificates) {	
		Stream<X509Certificate> certificatesStream = Arrays.stream(certificates);
		certificatesStream.forEach(certificate -> analyse(certificate));
	}
	
	public static void analyse(X509Certificate certificate) {	
		Principal principal = certificate.getIssuerDN();
		boolean hasCertificateExpired = false;
		try {
			certificate.checkValidity();
		}catch(CertificateException e) {
			hasCertificateExpired = true;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("Principal=").append(principal.getName()).append(",")
		.append("hasCertificateExpired=").append(hasCertificateExpired).append(",")
		.append("algorithm=").append(certificate.getSigAlgName());
		System.out.println(builder.toString());
	}
}
