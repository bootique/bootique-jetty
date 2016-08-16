package io.bootique.jetty.server;

import com.google.inject.Binder;
import com.google.inject.Module;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.unit.JettyApp;
import org.glassfish.jersey.message.GZipEncoder;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TlsConnectorIT {

    private static final String OUT_CONTENT = "https_content_stream_content_stream";

    @Rule
    public JettyApp app = new JettyApp();

    private Client client = ClientBuilder.newClient().register(GZipEncoder.class);

    @Test
    public void testTlsConnector() {
        app.startServer(new UnitModule(),
                "--config=classpath:io/bootique/jetty/server/TlsConnector.yml");

        Response r1HTTPS = client.target("https://localhost:14001/").request().get();
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
