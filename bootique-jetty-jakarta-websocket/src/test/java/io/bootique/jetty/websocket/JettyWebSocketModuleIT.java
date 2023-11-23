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
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JettyWebSocketModuleIT extends JettyWebSocketTestBase {

    @Test
    public void clientToServerMessage() throws IOException, DeploymentException, InterruptedException {

        BQRuntime runtime = testFactory.app("-s")
                .module(jetty.moduleReplacingConnectors())
                .module(b -> b.bind(ServerSocket1.class).inSingletonScope())
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(ServerSocket1.class))
                .createRuntime();

        runtime.run();

        ServerSocket1 serverSocket = runtime.getInstance(ServerSocket1.class);
        serverSocket.assertBuffer("");

        Session session = createClientSession("ws1");
        try {
            serverSocket.openLatch.await();
            serverSocket.assertBuffer("open;");

            session.getBasicRemote().sendText("hello");
            serverSocket.messageLatch.await();
            serverSocket.assertBuffer("open;message:hello;");
        } finally {
            session.close();
            serverSocket.closeLatch.await();
            serverSocket.assertBuffer("open;message:hello;close;");
        }
    }

    @Test
    public void decoderInAnnotation() throws IOException, DeploymentException, InterruptedException {

        BQRuntime runtime = testFactory.app("-s")
                .module(jetty.moduleReplacingConnectors())
                .module(b -> b.bind(ServerSocket2.class).inSingletonScope())
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(ServerSocket2.class))
                .createRuntime();

        runtime.run();

        ServerSocket2 serverSocket = runtime.getInstance(ServerSocket2.class);
        serverSocket.assertBuffer("");

        try (Session session = createClientSession("ws2")) {
            session.getBasicRemote().sendText("2018-03-01");
            serverSocket.messageLatch.await();
            serverSocket.assertBuffer("message:2018-03-01;");
        }
    }

    @ServerEndpoint(value = "/ws1")
    public static class ServerSocket1 {

        CountDownLatch openLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(1);
        CountDownLatch closeLatch = new CountDownLatch(1);

        StringBuilder buffer = new StringBuilder();

        void assertBuffer(String expected) {
            assertEquals(expected, buffer.toString());
        }

        @OnOpen
        public void onOpen() {
            buffer.append("open;");
            openLatch.countDown();
        }

        @OnMessage
        public void onMessageText(String message) {
            buffer.append("message:" + message + ";");
            messageLatch.countDown();
        }

        @OnClose
        public void onWebSocketClose() {
            buffer.append("close;");
            closeLatch.countDown();
        }

        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }

    @ServerEndpoint(value = "/ws2", decoders = DateDecoder.class)
    public static class ServerSocket2 {

        static StringBuilder buffer = new StringBuilder();
        CountDownLatch messageLatch = new CountDownLatch(1);

        void assertBuffer(String expected) {
            assertEquals(expected, buffer.toString());
        }

        @OnMessage
        public void onMessageText(LocalDate date) {
            buffer.append("message:" + date + ";");
            messageLatch.countDown();
        }

        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }

    public static class DateDecoder implements Decoder.Text<LocalDate> {

        @Override
        public LocalDate decode(String s) {
            return s != null ? LocalDate.parse(s) : null;
        }

        @Override
        public boolean willDecode(String s) {
            return true;
        }

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public void destroy() {
        }
    }
}
