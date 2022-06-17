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

package io.bootique.jetty.jakarta.server;

import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.server.ConnectorHolder;
import io.bootique.jetty.server.ServerHolder;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class HttpConnectorIT {

    private static final String OUT_CONTENT = "____content_stream____";

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @Test
    @DisplayName("multiple connectors")
    public void testMultipleConnectors() {

        BQRuntime app = startJetty("classpath:io/bootique/jetty/jakarta/server/HttpConnectorIT_multipleConnectors.yml");

        ServerHolder serverHolder = app.getInstance(ServerHolder.class);
        assertEquals(2, serverHolder.getConnectorsCount());

        Response r1 = ClientBuilder.newClient().target("http://localhost:14001/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals(OUT_CONTENT, r1.readEntity(String.class));

        Response r2 = ClientBuilder.newClient().target("http://localhost:14002/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals(OUT_CONTENT, r2.readEntity(String.class));
    }

    @Test
    @DisplayName("port: any")
    public void testDynamicPort() {

        BQRuntime app = startJetty("classpath:io/bootique/jetty/jakarta/server/HttpConnectorIT_dynamicPort.yml");

        ServerHolder serverHolder = app.getInstance(ServerHolder.class);
        ConnectorHolder connector = serverHolder.getConnector();
        int port = connector.getPort();

        assertTrue(port >= 1024);

        Response r = ClientBuilder.newClient().target("http://127.0.0.1:" + port + "/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals(OUT_CONTENT, r.readEntity(String.class));
    }

    private BQRuntime startJetty(String config) {
        BQRuntime runtime = testFactory.app("-s", "-c", config)
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
                .createRuntime();

        runtime.run();
        return runtime;
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().append(OUT_CONTENT);
        }
    }
}
