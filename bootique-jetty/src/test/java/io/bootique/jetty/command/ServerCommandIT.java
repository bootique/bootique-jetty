package io.bootique.jetty.command;

import io.bootique.command.CommandOutcome;
import io.bootique.jetty.JettyModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServerCommandIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testRun() {

        CommandOutcome outcome = testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "x", "/"))
                .run();

        assertTrue(outcome.isSuccess());
        assertTrue(outcome.forkedToBackground());

        // testing that the server is in the operational state by the time ServerCommand exits...
        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("Hello World!", r.readEntity(String.class));
    }

    public static class TestServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setHeader("content-type", "text/plain");
            resp.getWriter().append("Hello World!");
        }
    }
}
