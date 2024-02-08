package northwind.filter;

import java.io.IOException;
import java.security.cert.X509Certificate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.springframework.web.filter.GenericFilterBean;

import northwind.util.CertificateAnalyser;

public class AfterX509AuthenticationFilter extends GenericFilterBean{
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
    	X509Certificate[] certificates = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
    	CertificateAnalyser.analyse(certificates);
		chain.doFilter(request, response);
	}
}
