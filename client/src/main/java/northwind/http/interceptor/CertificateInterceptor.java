package northwind.http.interceptor;

import northwind.util.CertificateAnalyser;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.security.cert.X509Certificate;

@Component
public class CertificateInterceptor implements HttpResponseInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(CertificateInterceptor.class);
    @Override
    public void process(HttpResponse response, EntityDetails entity, HttpContext context) throws HttpException, IOException {
        SSLSession sslSession = (SSLSession) context.getAttribute(HttpCoreContext.SSL_SESSION);
        if (sslSession != null) {
            try {
                X509Certificate[] certificates = (X509Certificate[]) sslSession.getPeerCertificates();
                CertificateAnalyser.analyse(certificates);
            } catch (Exception e) {
                logger.warn("Failed to analyze certificates from SSL session: " + e.getMessage());
            }
        } else {
            logger.info("No SSL session found in context");
        }
    }
}
