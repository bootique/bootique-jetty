/**
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * “License”); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jetty.server;

import io.bootique.BQCoreModule;
import io.bootique.jetty.JettyModule;
import io.bootique.junit.BQTest;
import io.bootique.junit.BQTestFactory;
import io.bootique.junit.BQTestTool;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// The "Origin" header is a JDK-restricted header. It can only be sent because the test JVM fork is started with
// "-Dsun.net.http.allowRestrictedHeaders=true" (see maven-failsafe-plugin config in pom.xml).
@BQTest
public class CrossOriginHandlerIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void noCorsByDefault() {

        // no "jetty.cors" config => no CrossOriginHandler in the chain, hence no CORS headers
        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "test", "/*"))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("xp").request().header("Origin", "xo").options();
        assertNull(r.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    public void allOriginsByDefault() {

        // "jetty.cors" present but with no meaningful overrides (preflightMaxAge is set to its own default value just
        // to materialize the factory) => allowedOrigins defaults to * (reflecting the caller's Origin), no credentials
        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "test", "/*"))
                .module(b -> BQCoreModule.extend(b).setProperty("bq.jetty.cors.preflightMaxAge", "1800"))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("api").request().header("Origin", "anyorigin").options();
        assertEquals("anyorigin", r.getHeaderString("Access-Control-Allow-Origin"));

        // allowCredentials defaults to false, so the credentials header must be absent
        assertNull(r.getHeaderString("Access-Control-Allow-Credentials"));
    }

    @Test
    public void withCredentials() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "test", "/*"))
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.jetty.cors.allowedOrigins[0]", "test")
                        .setProperty("bq.jetty.cors.allowCredentials", "true"))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("api").request().header("Origin", "test").options();
        assertEquals("test", r.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", r.getHeaderString("Access-Control-Allow-Credentials"));
    }

    @Test
    public void byOrigin() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "test", "/*"))
                .module(b -> BQCoreModule.extend(b).setProperty("bq.jetty.cors.allowedOrigins[0]", "test"))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("api").request().header("Origin", "test").options();
        assertEquals("test", r1.getHeaderString("Access-Control-Allow-Origin"));

        Response r2 = base.path("api").request().header("Origin", "test2").options();
        assertNull(r2.getHeaderString("Access-Control-Allow-Origin"));

        Response r3 = base.path("api").request().options();
        assertNull(r3.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    public void byPath() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "test", "/*"))
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.jetty.cors.allowedOrigins[0]", "test")
                        .setProperty("bq.jetty.cors.urlPatterns[0]", "/api"))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("api").request().header("Origin", "test").options();
        assertEquals("test", r1.getHeaderString("Access-Control-Allow-Origin"));

        Response r2 = base.path("notapi").request().header("Origin", "test").options();
        assertNull(r2.getHeaderString("Access-Control-Allow-Origin"));
    }

    static class TestServlet extends HttpServlet {

        @Override
        protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
            resp.setStatus(200);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            resp.setStatus(200);
        }
    }
}
