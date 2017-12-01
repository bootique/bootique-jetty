package io.bootique.jetty;

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
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Demonstrates how one can define custom error handlers.
 */
public class ErrorPagesIT {

    static final String CUSTOM_404_RESPONSE = "custom 404 response";

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void errorPagesHandlerCaptures404Request() {
        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/error-pages.yml")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(new CustomErrorHandler(), "error-handler", "/not-found-handler"))
                .createRuntime()
                .run();

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
