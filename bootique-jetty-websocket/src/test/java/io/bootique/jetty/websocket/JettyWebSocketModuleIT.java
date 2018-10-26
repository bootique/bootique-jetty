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
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.URI;
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

    private Session createClientSession() throws IOException, DeploymentException {
        return container.connectToServer(TestSocket.class, URI.create("ws://127.0.0.1:8080/wstest/"));
    }

    @Test
    public void testBasicCommunication() throws IOException, DeploymentException, InterruptedException {

        BQRuntime runtime = testFactory.app("-s")
                .autoLoadModules()
                .module(b -> b.bind(TestSocket.class).in(Singleton.class))
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(TestSocket.class))
                .createRuntime();

        runtime.run();

        TestSocket serverSocket = runtime.getInstance(TestSocket.class);
        serverSocket.assertBuffer("");

        Session session = createClientSession();
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

    @ClientEndpoint
    @ServerEndpoint(value = "/wstest/")
    public static class TestSocket {

        CountDownLatch openLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(1);
        CountDownLatch closeLatch = new CountDownLatch(1);

        StringBuilder buffer = new StringBuilder();

        void assertBuffer(String expected) {
            assertEquals(expected, buffer.toString());
        }

        @OnOpen
        public void onOpen(Session session) {
            buffer.append("open;");
            openLatch.countDown();
        }

        @OnMessage
        public void onMessageText(String message) {
            buffer.append("message:" + message + ";");
            messageLatch.countDown();
        }

        @OnClose
        public void onWebSocketClose(CloseReason reason) {
            buffer.append("close;");
            closeLatch.countDown();
        }

        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }
}
