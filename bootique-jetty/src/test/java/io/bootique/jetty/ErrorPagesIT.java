package io.bootique.jetty;

import io.bootique.jetty.unit.JettyApp;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Demonstrates how one can define custom error handlers.
 *
 * @author Lukasz Bachman
 */
public class ErrorPagesIT {

    static final String CUSTOM_404_RESPONSE = "custom 404 response";

    @Rule
    public JettyApp app = new JettyApp();

    @Test
    public void errorPagesHandlerCaptures404Request() {
        app.start(binder -> JettyModule.extend(binder).addServlet(
                new CustomErrorHandler(),
                "error-handler",
                "/not-found-handler"),
                "--config=src/test/resources/io/bootique/jetty/error-pages.yml"
        );

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/this-page-does-not-exist").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r1.getStatus());
        assertEquals("custom 404 response", r1.readEntity(String.class));
    }

    static class CustomErrorHandler extends HttpServlet {
        private static final long serialVersionUID = -3190255883516320766L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            // Outputs custom message instead of the default 404.
            resp.getWriter().print(CUSTOM_404_RESPONSE);
        }
    }
}
