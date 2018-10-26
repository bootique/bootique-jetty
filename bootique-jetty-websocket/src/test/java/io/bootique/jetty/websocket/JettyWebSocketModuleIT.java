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
import io.bootique.test.junit.BQTestFactory;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class JettyWebSocketModuleIT {

    @Rule
    public final BQTestFactory testFactory = new BQTestFactory();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    private WebSocketContainer container;

    @Before
    public void startClient() {
        container = ContainerProvider.getWebSocketContainer();
    }

    @After
    public void stopClient() throws Exception {
        // some jetty thing... see:
        // https://github.com/jetty-project/embedded-jetty-websocket-examples/blob/master/javax.websocket-example/src/main/java/org/eclipse/jetty/demo/EventClient.java#L32
        ((LifeCycle) container).stop();
    }

    private Session createClientSession(String path) throws IOException, DeploymentException {
        return container.connectToServer(ClientSocket1.class, URI.create("ws://127.0.0.1:8080/" + path));
    }

    @Test
    public void testClientServerMessage() throws IOException, DeploymentException, InterruptedException {

        BQRuntime runtime = testFactory.app("-s")
                .autoLoadModules()
                .module(b -> b.bind(ServerSocket1.class).in(Singleton.class))
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
    public void testDecoder() throws IOException, DeploymentException, InterruptedException {

        BQRuntime runtime = testFactory.app("-s")
                .autoLoadModules()
                .module(b -> b.bind(ServerSocket2.class).in(Singleton.class))
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(ServerSocket2.class))
                .createRuntime();

        runtime.run();

        ServerSocket2 serverSocket = runtime.getInstance(ServerSocket2.class);
        serverSocket.assertBuffer("");

        Session session = createClientSession("ws2");
        try {
            session.getBasicRemote().sendText("2018-03-01");
            serverSocket.messageLatch.await();
            serverSocket.assertBuffer("message:2018-03-01;");
        } finally {
            session.close();
        }
    }

    @Test
    public void testEndpointScope() throws IOException, DeploymentException, InterruptedException {

        BQRuntime runtime = testFactory.app("-s")
                .autoLoadModules()
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(ServerSocket3.class))
                .createRuntime();

        runtime.run();

        ServerSocket3.assertInstances(0);

        for (int i = 1; i <= 3; i++) {
            Session session = createClientSession("ws3");
            try {
                ServerSocket3.openLatch.await();
            } finally {
                session.close();
            }

            ServerSocket3.assertInstances(i);
        }
    }

    @ClientEndpoint
    public static class ClientSocket1 {
        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
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

    @ServerEndpoint(value = "/ws3")
    public static class ServerSocket3 {

        static Map<ServerSocket3, Integer> instanceMap = new ConcurrentHashMap<>();
        static CountDownLatch openLatch = new CountDownLatch(1);

        static void assertInstances(int expected) {
            assertEquals(expected, instanceMap.size());
        }

        @OnOpen
        public void onOpen() {
            instanceMap.put(this, 1);
            openLatch.countDown();
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
