package northwind.filter;

import java.io.IOException;
import java.security.cert.X509Certificate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.web.filter.GenericFilterBean;

import northwind.util.CertificateAnalyser;

public class AfterX509AuthenticationFilter extends GenericFilterBean{

	// Jakarta EE attribute name for X509 certificates
	private static final String X509_CERTIFICATE_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";
	// Fallback to older attribute name for compatibility
	private static final String LEGACY_X509_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		X509Certificate[] certificates = null;

		Object certAttribute = request.getAttribute(X509_CERTIFICATE_ATTRIBUTE);
		if (certAttribute == null) {
			// Fallback to legacy attribute name for backwards compatibility
			certAttribute = request.getAttribute(LEGACY_X509_CERTIFICATE_ATTRIBUTE);
		}

		if (certAttribute instanceof X509Certificate[]) {
			certificates = (X509Certificate[]) certAttribute;
		}

		if (ArrayUtils.isNotEmpty(certificates)) {
			try {
				CertificateAnalyser.analyse(certificates);
			} catch (Exception e) {
				logger.warn("Failed to analyze X509 certificates: " + e.getMessage(), e);
			}
		} else {
			logger.info("No X509 certificates found in request");
		}

		chain.doFilter(request, response);
	}
}
