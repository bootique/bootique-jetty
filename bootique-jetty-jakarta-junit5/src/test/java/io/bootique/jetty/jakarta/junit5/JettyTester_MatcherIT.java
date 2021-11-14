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
package io.bootique.jetty.jakarta.junit5;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.jakarta.JettyModule;
import io.bootique.jetty.jakarta.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class JettyTester_MatcherIT {

    private static final String OUT_CONTENT = "____content_stream____";

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @Test
    public void testMatch200() {
        Response r1 = jetty.getTarget().request().get();
        JettyTester.matcher(r1).assertOk().assertContent(OUT_CONTENT);

        // try a different style
        Response r2 = jetty.getTarget().request().get();
        JettyTester.assertOk(r2).assertContent(OUT_CONTENT);
    }

    @Test
    public void testMatch200_ContentType() {
        Response r1 = jetty.getTarget().queryParam("wantedType", "application/json").request().get();
        JettyTester.matcher(r1).assertOk().assertContentType(MediaType.APPLICATION_JSON_TYPE);

        // non-standard type
        Response r2 = jetty.getTarget().queryParam("wantedType", "application/geo+json").request().get();
        JettyTester.matcher(r2).assertOk().assertContentType(new MediaType("application", "*"));

        // string comparision
        Response r3 = jetty.getTarget().queryParam("wantedType", "application/geo+json").request().get();
        JettyTester.matcher(r3).assertOk().assertContentType("application/*");
    }

    @Test
    public void testMatch200_CustomAssertions() {
        Response r = jetty.getTarget().request().get();
        JettyTester.matcher(r).assertOk().assertContent(c -> {
            assertNotNull(c);
            assertTrue(c.contains("_stream_"));
        });
    }

    @Test
    public void testMatch400() {
        Response r1 = jetty.getTarget().queryParam("wantedStatus", "404").request().get();
        JettyTester.matcher(r1).assertNotFound();

        // try a different style
        Response r2 = jetty.getTarget().queryParam("wantedStatus", "404").request().get();
        JettyTester.assertNotFound(r2);
    }

    @Test
    public void testMatch500() {
        Response r1 = jetty.getTarget().queryParam("wantedStatus", "500").request().get();
        JettyTester.matcher(r1).assertStatus(500);

        // try a different style
        Response r2 = jetty.getTarget().queryParam("wantedStatus", "500").request().get();
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
