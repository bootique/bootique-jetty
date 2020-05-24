package io.bootique.jetty.server;

import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

public class SendServerVersionIT {

    private static final String OUT_CONTENT = "xcontent_stream_content_stream";

    @RegisterExtension
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private Client client = ClientBuilder.newClient();

    @Test
    public void testSendServerHeaderOn() {
        BQRuntime runtime = testFactory
                .app("-s", "-c", "classpath:io/bootique/jetty/server/sendServerVersion.yml")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ServerFactoryConnectorsIT.ContentServlet.class))
                .createRuntime();
        runtime.run();

        Response r1NormalConnector = client.target("http://localhost:14001/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1NormalConnector.getStatus());
        assertEquals(OUT_CONTENT, r1NormalConnector.readEntity(String.class));

        MultivaluedMap<String, Object> headers = r1NormalConnector.getHeaders();
        assertEquals(4, headers.size());
        assertNotNull(headers.get("Server"));
    }

    @Test
    public void testSendServerHeaderOff() {
        BQRuntime runtime = testFactory
                .app("-s", "-c", "classpath:io/bootique/jetty/server/sendServerVersion.yml")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ServerFactoryConnectorsIT.ContentServlet.class))
                .createRuntime();
        runtime.run();

        Response r1NormalConnector = client.target("http://localhost:14002/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1NormalConnector.getStatus());
        assertEquals(OUT_CONTENT, r1NormalConnector.readEntity(String.class));

        MultivaluedMap<String, Object> headers = r1NormalConnector.getHeaders();
        assertEquals(3, headers.size());
        assertNull(headers.get("Server"));
    }

}
