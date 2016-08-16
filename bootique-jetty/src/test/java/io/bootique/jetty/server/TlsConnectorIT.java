package io.bootique.jetty.server;

import com.google.inject.Binder;
import com.google.inject.Module;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.unit.JettyApp;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.junit.Assert.assertEquals;

public class TlsConnectorIT {

    private static final String OUT_CONTENT = "https_content_stream_content_stream";
    private static final String SERVICE_URL = "https://localhost:14001/";

    @Rule
    public JettyApp app = new JettyApp();

    private WebTarget createHttpsClient(String keystore) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        KeyStore trustStore;

        try (InputStream in = getClass().getResourceAsStream(keystore)) {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(in, "supersecret".toCharArray());
        }

        return ClientBuilder.newBuilder().trustStore(trustStore).build().target(SERVICE_URL);
    }

    @Test
    public void testTlsConnector() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        app.startServer(new UnitModule(),
                "--config=classpath:io/bootique/jetty/server/TlsConnector.yml");

        Response r1HTTPS = createHttpsClient("testkeystore").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1HTTPS.getStatus());
        assertEquals(OUT_CONTENT, r1HTTPS.readEntity(String.class));
    }

    @Test
    public void testTlsConnector_MultiCert() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        app.startServer(new UnitModule(),
                "--config=classpath:io/bootique/jetty/server/TlsMultiCertConnector.yml");

        // TODO: how do we verify that "jetty2" certificate was used, and noth "jetty1"?

        Response r1HTTPS = createHttpsClient("testmulticertkeystore").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1HTTPS.getStatus());
        assertEquals(OUT_CONTENT, r1HTTPS.readEntity(String.class));
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.getWriter().append(OUT_CONTENT);
        }
    }

    private class UnitModule implements Module {

        @Override
        public void configure(Binder binder) {
            JettyModule.contributeServlets(binder).addBinding().to(ContentServlet.class);
        }
    }
}
