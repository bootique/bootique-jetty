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
package io.bootique.jetty.websocket;

import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import org.junit.jupiter.api.Test;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.DeploymentException;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebSocketServletMixIT extends JettyWebSocketTestBase {

    @Test
    public void testServletCoexistence() throws IOException, DeploymentException, InterruptedException {

        BQRuntime runtime = testFactory.app("-s")
                .module(jetty.moduleReplacingConnectors())
                .module(b -> JettyModule.extend(b).addServlet(ServletSamePath.class).addServlet(ServletDifferentPath.class))
                .module(b -> b.bind(ServerSocket.class).inSingletonScope())
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(ServerSocket.class))
                .createRuntime();

        runtime.run();

        ServerSocket serverSocket = runtime.getInstance(ServerSocket.class);
        try (Session session = createClientSession("socket")) {
            session.getBasicRemote().sendText("socket accessed");
            serverSocket.messageLatch.await();
            serverSocket.assertBuffer(";socket accessed");
        }

        Response r1 = jetty.getTarget().path("servlet").request().get();
        assertEquals(200, r1.getStatus());
        assertEquals("servlet accessed as /servlet", r1.readEntity(String.class));

        // same path as websocket here.. resolution will happen at the protocol level
        Response r2 = jetty.getTarget().path("socket").request().get();
        assertEquals(200, r2.getStatus());
        assertEquals("servlet accessed as /socket", r2.readEntity(String.class));
    }

    @WebServlet(urlPatterns = "/socket")
    public static class ServletSamePath extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().print("servlet accessed as /socket");
        }
    }

    @WebServlet(urlPatterns = "/servlet")
    public static class ServletDifferentPath extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().print("servlet accessed as /servlet");
        }
    }

    @ServerEndpoint(value = "/socket")
    public static class ServerSocket {

        CountDownLatch messageLatch = new CountDownLatch(1);
        StringBuilder buffer = new StringBuilder();

        void assertBuffer(String expected) {
            assertEquals(expected, buffer.toString());
        }

        @OnMessage
        public void onMessageText(String message) {
            buffer.append(";" + message);
            messageLatch.countDown();
        }

        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }
}
