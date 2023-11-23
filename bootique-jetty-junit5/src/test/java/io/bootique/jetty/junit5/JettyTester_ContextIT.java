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

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class JettyTester_ContextIT {

    private static final String OUT_CONTENT = "____content_stream____";

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b).setProperty("bq.jetty.context", "myapp"))
            .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
            .module(jetty.moduleReplacingConnectors())
            // for predictable URL assertions
            .module(b -> BQCoreModule.extend(b).setProperty("bq.jetty.connectors[0].host", "127.0.0.1"))
            .createRuntime();

    @Test
    public void getServerUrl() {
        String url = jetty.getUrl();
        Assertions.assertNotNull(url);

        assertEquals("http://127.0.0.1:" + jetty.getPort() + "/myapp", url);
    }

    @Test
    public void getTarget() {
        WebTarget client = jetty.getTarget();
        Assertions.assertNotNull(client);

        assertEquals("http://127.0.0.1:" + jetty.getPort() + "/myapp", client.getUri().toString());

        Response r = client.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals(OUT_CONTENT, r.readEntity(String.class));
    }

    @Test
    public void getTarget_NoRedirects() {
        WebTarget client = jetty.getTarget(false);

        Response r = client
                // trailing "/" is needed to prevent servlet redirect
                .path("/")
                .queryParam("redirect", "true").request().get();

        JettyTester.assertTempRedirect(r).assertHeader("Location", "/someurl");
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

            if ("true".equals(req.getParameter("redirect"))) {
                resp.setStatus(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
                resp.setHeader("Location", "/someurl");
            } else {
                resp.getWriter().append(OUT_CONTENT);
            }
        }
    }
}
