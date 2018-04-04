package io.bootique.jetty.server;

import io.bootique.jetty.JettyModule;
import io.bootique.test.junit.BQTestFactory;
import org.glassfish.jersey.message.GZipEncoder;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MaxFormSettingsIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    private WebTarget target = ClientBuilder.newClient().register(GZipEncoder.class)
            .target("http://localhost:8080/");

    @Test
    public void testMaxFormContentSize() {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/server/MaxFormSettingsIT_10b_request.yml")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
                .run();

        Response belowThreshold = target
                .request()
                .post(Entity.entity("a=1234567", MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        assertEquals(200, belowThreshold.getStatus());
        assertEquals("[1234567]", belowThreshold.readEntity(String.class));

        Response aboveThreshold = target
                .request()
                .post(Entity.entity("a=123456789", MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        assertEquals(400, aboveThreshold.getStatus());

        // the size limit includes the full form with keys and "=" signs...
        Response atThreshold = target
                .request()
                .post(Entity.entity("a=12345678", MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        assertEquals(200, atThreshold.getStatus());
        assertEquals("[12345678]", atThreshold.readEntity(String.class));
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String a = req.getParameter("a");
            resp.getWriter().append("[" + a + "]");
        }
    }
}
