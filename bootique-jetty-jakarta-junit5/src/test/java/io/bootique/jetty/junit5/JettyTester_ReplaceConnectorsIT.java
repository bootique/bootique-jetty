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
import io.bootique.command.CommandOutcome;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.jetty.server.ServerHolder;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class JettyTester_ReplaceConnectorsIT {

    private static final String OUT_CONTENT = "____content_stream____";

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory();

    private static ServerSocket _42001;
    private static ServerSocket _42002;

    @BeforeAll
    public static void blockPorts() throws IOException {
        // on the off chance that the random port assignment would match one of the overridden ports, let's block
        // them explicitly
        InetAddress localhost = InetAddress.getLocalHost();
        _42001 = ServerSocketFactory.getDefault().createServerSocket(42001, 1, localhost);
        _42002 = ServerSocketFactory.getDefault().createServerSocket(42002, 1, localhost);
    }

    @AfterAll
    public static void unblockPorts() {
        try {
            _42001.close();
        } catch (IOException e) {
            // ignore
        }

        try {
            _42002.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Test
    @DisplayName("Connectors from YAML must be replaced by the test connector")
    public void testModuleReplacingConnectors() {

        JettyTester jetty = JettyTester.create();

        BQRuntime app = testFactory
                .app("-s", "-c", "classpath:io/bootique/jetty/jakarta/junit5/JettyTester_ReplaceConnectorsIT.yml")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
                .module(jetty.moduleReplacingConnectors())
                .createRuntime();

        ServerHolder serverHolder = app.getInstance(ServerHolder.class);
        assertEquals(1, serverHolder.getConnectorsCount());
        assertNotEquals(42001, serverHolder.getConnector().getPort());
        assertNotEquals(42002, serverHolder.getConnector().getPort());

        CommandOutcome out = app.run();
        assertTrue(out.isSuccess());

        WebTarget client = jetty.getTarget();

        Response r = client.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals(OUT_CONTENT, r.readEntity(String.class));
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().append(OUT_CONTENT);
        }
    }
}
