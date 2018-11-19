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
package io.bootique.jetty.cors;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Binder;
import com.google.inject.Module;
import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.test.junit.BQTestFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BootiqueCorsFilterIT {

    private static final String OUT_CONTENT = "xcontent_stream_content_stream";

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test(expected = RuntimeException.class)
    public void testLoadFilter() {

        BQRuntime runtime = testFactory
                .app("-s", "-c", "classpath:io/bootique/jetty/cors/NoCorsFilter.yml")
                .module(new UnitModule())
                .createRuntime();

        runtime.run();
    }

    @Test
    public void testResponseHeaders() throws Exception {
        BQRuntime runtime = testFactory
                .app("-s", "-c", "classpath:io/bootique/jetty/cors/CorsFilter.yml")
                .module(JettyServletsModule.class)
                .module(new UnitModule())
                .createRuntime();

        runtime.run();

        HttpClient httpClient = new HttpClient();
        httpClient.start();

        // deprecated default connector must NOT be started
        Server server = runtime.getInstance(Server.class);
        Connector[] connectors = server.getConnectors();
        assertEquals(2, connectors.length);

        ContentResponse response1 = httpClient.newRequest("http://localhost:15001/api")
                .header("Origin", "test")
                .method(HttpMethod.OPTIONS).send();
        assertEquals("test", response1.getHeaders().get("Access-Control-Allow-Origin"));

        ContentResponse response2 = httpClient.newRequest("http://localhost:15001/api")
                .header("Origin", "test2")
                .method(HttpMethod.OPTIONS).send();
        assertNull(response2.getHeaders().get("Access-Control-Allow-Origin"));

        ContentResponse response3 = httpClient.newRequest("http://localhost:15001/api")
                .method(HttpMethod.OPTIONS).send();
        assertNull(response3.getHeaders().get("Access-Control-Allow-Origin"));
    }

    @WebServlet(urlPatterns = "/api")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.getWriter().append(OUT_CONTENT);
        }
    }

    class UnitModule implements Module {

        @Override
        public void configure(Binder binder) {
            JettyModule.extend(binder).addServlet(ContentServlet.class);
        }
    }
}
