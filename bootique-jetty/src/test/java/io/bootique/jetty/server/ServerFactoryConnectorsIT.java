package io.bootique.jetty.server;

import com.google.inject.Binder;
import com.google.inject.Module;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.unit.JettyApp;
import io.bootique.test.BQDaemonTestRuntime;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
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

public class ServerFactoryConnectorsIT {

    private static final String OUT_CONTENT = "xcontent_stream_content_stream";

    @Rule
    public JettyApp app = new JettyApp();

    private Client client = ClientBuilder.newClient();

    @Test
    public void testMultipleConnectors_WithDeprecated() {
        BQDaemonTestRuntime runtime = app.startServer(new UnitModule(),
                "--config=classpath:io/bootique/jetty/server/connectors-deprecated.yml");

        // deprecated default connector must NOT be started
        Connector[] connectors = runtime.getRuntime().getInstance(Server.class).getConnectors();
        assertEquals(3, connectors.length);

        Response r1LegacyConnector = client.target("http://localhost:14001/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1LegacyConnector.getStatus());
        assertEquals(OUT_CONTENT, r1LegacyConnector.readEntity(String.class));

        Response r2NormalConnector = client.target("http://localhost:14002/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2NormalConnector.getStatus());
        assertEquals(OUT_CONTENT, r2NormalConnector.readEntity(String.class));

        Response r3NormalConnector = client.target("http://localhost:14003/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r3NormalConnector.getStatus());
        assertEquals(OUT_CONTENT, r3NormalConnector.readEntity(String.class));
    }

    @Test
    public void testMultipleConnectors() {

        BQDaemonTestRuntime runtime = app.startServer(new UnitModule(),
                "--config=classpath:io/bootique/jetty/server/connectors.yml");

        // deprecated default connector must NOT be started
        Connector[] connectors = runtime.getRuntime().getInstance(Server.class).getConnectors();
        assertEquals(2, connectors.length);

        Response r1NormalConnector = client.target("http://localhost:14001/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1NormalConnector.getStatus());
        assertEquals(OUT_CONTENT, r1NormalConnector.readEntity(String.class));

        Response r2NormalConnector = client.target("http://localhost:14002/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2NormalConnector.getStatus());
        assertEquals(OUT_CONTENT, r2NormalConnector.readEntity(String.class));
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        private static final long serialVersionUID = -8896839263652092254L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.getWriter().append(OUT_CONTENT);
        }
    }

    class UnitModule implements Module {

        @Override
        public void configure(Binder binder) {
            JettyModule.contributeServlets(binder).addBinding().to(ContentServlet.class);
        }
    }
}
