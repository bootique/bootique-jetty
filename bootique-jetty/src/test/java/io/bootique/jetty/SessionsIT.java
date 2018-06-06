/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
