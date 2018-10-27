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

import com.google.inject.Singleton;
import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import org.junit.Test;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.DeploymentException;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class WebSocketServletMixIT extends JettyWebSocketTestBase {

    protected WebTarget createWebTarget(String path) {
        return ClientBuilder.newClient().target("http://127.0.0.1:8080/" + path);
    }

    @Test
    public void testServletCoexistence() throws IOException, DeploymentException, InterruptedException {

        BQRuntime runtime = testFactory.app("-s")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(Servlet.class))
                .module(b -> b.bind(ServerSocket.class).in(Singleton.class))
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(ServerSocket.class))
                .createRuntime();

        runtime.run();

        ServerSocket serverSocket = runtime.getInstance(ServerSocket.class);
        try (Session session = createClientSession("socket")) {
            session.getBasicRemote().sendText("socket accessed");
            serverSocket.messageLatch.await();
            serverSocket.assertBuffer(";socket accessed");
        }

        Response r = createWebTarget("servlet").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("servlet accessed", r.readEntity(String.class));
    }

    @WebServlet(urlPatterns = "/servlet")
    public static class Servlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().print("servlet accessed");
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
