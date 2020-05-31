/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.jetty.junit5;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class JettyTester_MatcherIT {

    private static final String OUT_CONTENT = "____content_stream____";

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
            .module(JettyTester.moduleReplacingConnectors())
            .createRuntime();

    @Test
    public void testMatch200() {
        Response r1 = JettyTester.getTarget(app).request().get();
        JettyTester.matcher(r1).assertOk().assertContent(OUT_CONTENT);

        // try a different style
        Response r2 = JettyTester.getTarget(app).request().get();
        JettyTester.assertOk(r2).assertContent(OUT_CONTENT);
    }

    @Test
    public void testMatch200_ContentType() {
        Response r1 = JettyTester.getTarget(app).queryParam("wantedType", "application/json").request().get();
        JettyTester.matcher(r1).assertOk().assertContentType(MediaType.APPLICATION_JSON_TYPE);

        // non-standard type
        Response r2 = JettyTester.getTarget(app).queryParam("wantedType", "application/geo+json").request().get();
        JettyTester.matcher(r2).assertOk().assertContentType(new MediaType("application", "*"));

        // string comparision
        Response r3 = JettyTester.getTarget(app).queryParam("wantedType", "application/geo+json").request().get();
        JettyTester.matcher(r3).assertOk().assertContentType("application/*");
    }

    @Test
    public void testMatch200_CustomAssertions() {
        Response r = JettyTester.getTarget(app).request().get();
        JettyTester.matcher(r).assertOk().assertContent(c -> {
            assertNotNull(c);
            assertTrue(c.contains("_stream_"));
        });
    }

    @Test
    public void testMatch400() {
        Response r1 = JettyTester.getTarget(app).queryParam("wantedStatus", "404").request().get();
        JettyTester.matcher(r1).assertNotFound();

        // try a different style
        Response r2 = JettyTester.getTarget(app).queryParam("wantedStatus", "404").request().get();
        JettyTester.assertNotFound(r2);
    }

    @Test
    public void testMatch500() {
        Response r1 = JettyTester.getTarget(app).queryParam("wantedStatus", "500").request().get();
        JettyTester.matcher(r1).assertStatus(500);

        // try a different style
        Response r2 = JettyTester.getTarget(app).queryParam("wantedStatus", "500").request().get();
        JettyTester.assertStatus(r2, 500);
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

            String statusString = req.getParameter("wantedStatus");
            int status = statusString != null ? Integer.parseInt(statusString) : 200;

            if(status == 200) {

                String contentType = req.getParameter("wantedType");
                if(contentType != null) {
                    resp.setContentType(contentType);
                }

                resp.getWriter().append(OUT_CONTENT);
            }
            else {
                resp.setStatus(status);
            }
        }
    }
}
