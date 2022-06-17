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
package io.bootique.jetty.jakarta.websocket;

import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.websocket.*;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.URI;

@BQTest
@Timeout(10)
public abstract class JettyWebSocketTestBase {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    final JettyTester jetty = JettyTester.create();

    protected WebSocketContainer client;

    protected Session createClientSession(String path) throws IOException, DeploymentException {
        return createClientSession(ClientSocket.class, path);
    }

    protected Session createClientSession(Class<?> clientSocketType, String path) throws IOException, DeploymentException {
        return client.connectToServer(clientSocketType, URI.create("ws://127.0.0.1:" + jetty.getPort() + "/" + path));
    }

    @BeforeEach
    public void startClient() {
        client = ContainerProvider.getWebSocketContainer();
    }

    @AfterEach
    public void stopClient() throws Exception {
        // some jetty thing... see:
        // https://github.com/jetty-project/embedded-jetty-websocket-examples/blob/master/javax.websocket-example/src/main/java/org/eclipse/jetty/demo/EventClient.java#L32
        ((LifeCycle) client).stop();
    }

    @ClientEndpoint
    public static class ClientSocket {
        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }
}
