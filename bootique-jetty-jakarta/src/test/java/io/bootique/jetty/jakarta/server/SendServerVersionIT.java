package io.bootique.jetty.jakarta.server;

import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class SendServerVersionIT {

    private static final String OUT_CONTENT = "____content_stream____";

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private Client client = ClientBuilder.newClient();

    @Test
    public void testSendServerHeaderOn() {
        startJetty("classpath:io/bootique/jetty/jakarta/server/sendServerVersion.yml");

        Response r = client.target("http://localhost:14001/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals(OUT_CONTENT, r.readEntity(String.class));

        MultivaluedMap<String, Object> headers = r.getHeaders();
        assertEquals(4, headers.size());
        assertNotNull(headers.get("Server"));
    }

    @Test
    public void testSendServerHeaderOff() {
        startJetty("classpath:io/bootique/jetty/jakarta/server/sendServerVersion.yml");

        Response r = client.target("http://localhost:14002/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals(OUT_CONTENT, r.readEntity(String.class));

        MultivaluedMap<String, Object> headers = r.getHeaders();
        assertEquals(3, headers.size());
        assertNull(headers.get("Server"));
    }

    private void startJetty(String config) {
        testFactory.app("-s", "-c", config)
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
                .run();
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().append(OUT_CONTENT);
        }
    }
}
