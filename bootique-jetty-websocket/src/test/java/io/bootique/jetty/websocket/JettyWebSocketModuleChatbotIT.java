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

import io.bootique.command.CommandOutcome;
import org.junit.jupiter.api.Test;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JettyWebSocketModuleChatbotIT extends JettyWebSocketTestBase {

    @Test
    public void testTwoWayMessaging() throws IOException, DeploymentException, InterruptedException {

        CommandOutcome serverStatus = testFactory.app("-s")
                .module(jetty.moduleReplacingConnectors())
                .module(b -> b.bind(ChatServer.class).inSingletonScope())
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(ChatServer.class))
                .run();

        assertTrue(serverStatus.isSuccess(), () -> serverStatus.toString());

        try (Session s1 = createClientSession(ChatClient1.class, "chat")) {
            try (Session s2 = createClientSession(ChatClient2.class, "chat")) {
                s1.getBasicRemote().sendText("from s1");

                ChatClient2.messageLatch.await();
                ChatClient2.assertBuffer(";from s1");
            }
        }
    }

    @ClientEndpoint
    public static class ChatClient1 {

        static CountDownLatch messageLatch = new CountDownLatch(1);

        static StringBuilder buffer = new StringBuilder();

        @OnMessage
        public void onMessage(String message) {
            buffer.append(";").append(message);
            messageLatch.countDown();
        }

        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }

    @ClientEndpoint
    public static class ChatClient2 {

        static CountDownLatch messageLatch = new CountDownLatch(1);

        static StringBuilder buffer = new StringBuilder();

        static void assertBuffer(String expected) {
            assertEquals(expected, buffer.toString());
        }

        @OnMessage
        public void onMessage(String message) {
            buffer.append(";").append(message);
            messageLatch.countDown();
        }

        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }

    @ServerEndpoint("/chat")
    public static class ChatServer {

        private Map<Session, Integer> sessions = new ConcurrentHashMap<>();

        @OnOpen
        public void onOpen(Session session) {
            sessions.put(session, 1);
        }

        @OnMessage
        public void onMessage(String message) {
            sessions.keySet().forEach(s -> sendTo(s, message));
        }

        private void sendTo(Session session, String message) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @OnClose
        public void onClose(Session session) {
            sessions.remove(session);
        }

        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }
}
