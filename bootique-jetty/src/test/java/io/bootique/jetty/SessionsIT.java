package io.bootique.jetty;

import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SessionsIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testSessions() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "s1", "/*"))
                .autoLoadModules()
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("count: 1", r1.readEntity(String.class));
        NewCookie sessionId = r1.getCookies().get("JSESSIONID");

        assertNotNull(sessionId);

        Response r2 = base.path("/").request().cookie(sessionId).get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("count: 2", r2.readEntity(String.class));
    }

    @Test
    public void testNoSessions() {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/nosessions.yml")
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "s1", "/*"))
                .autoLoadModules()
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("nosessions", r1.readEntity(String.class));
    }

    static class TestServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            String message;

            try {

                // this throws if sessions are disabled
                HttpSession session = req.getSession(true);

                Integer count = (Integer) session.getAttribute("count");
                count = count != null ? count + 1 : 1;
                session.setAttribute("count", count);
                message = "count: " + count;
            } catch (IllegalStateException e) {
                message = "nosessions";
            }

            resp.setContentType("text/plain");
            resp.getWriter().print(message);
        }
    }

}
