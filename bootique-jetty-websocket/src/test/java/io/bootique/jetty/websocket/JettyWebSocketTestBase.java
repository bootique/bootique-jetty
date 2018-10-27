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

import io.bootique.test.junit.BQTestFactory;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnError;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;

public abstract class JettyWebSocketTestBase {

    @Rule
    public final BQTestFactory testFactory = new BQTestFactory();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    protected WebSocketContainer client;

    protected Session createClientSession(String path) throws IOException, DeploymentException {
        return createClientSession(ClientSocket.class, path);
    }

    protected Session createClientSession(Class<?> clientSocketType, String path) throws IOException, DeploymentException {
        return client.connectToServer(clientSocketType, URI.create("ws://127.0.0.1:8080/" + path));
    }

    @Before
    public void startClient() {
        client = ContainerProvider.getWebSocketContainer();
    }

    @After
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
