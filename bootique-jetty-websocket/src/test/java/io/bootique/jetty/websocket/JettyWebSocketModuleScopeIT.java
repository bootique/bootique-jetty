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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.websocket.DeploymentException;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JettyWebSocketModuleScopeIT extends JettyWebSocketTestBase {

    @BeforeEach
    public void reset() {
        ServerSocket.reset();
    }

    @Test
    public void testEndpointScopeNone() throws IOException, DeploymentException, InterruptedException {

        BQRuntime runtime = testFactory.app("-s")
                .autoLoadModules()
                // binding endpoint with no scope
                .module(b -> b.bind(ServerSocket.class))
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(ServerSocket.class))
                .createRuntime();

        runtime.run();

        for (int i = 1; i <= 3; i++) {

            ServerSocket.resetLatch();

            try (Session session = createClientSession("ws3")) {
                ServerSocket.openLatch.await();
            }

            ServerSocket.assertInstances(i);
        }
    }

    @Test
    public void testEndpointScopeSingleton() throws IOException, DeploymentException, InterruptedException {

        BQRuntime runtime = testFactory.app("-s")
                .autoLoadModules()
                // binding with singleton scope
                .module(b -> b.bind(ServerSocket.class).inSingletonScope())
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(ServerSocket.class))
                .createRuntime();

        runtime.run();

        for (int i = 1; i <= 3; i++) {

            ServerSocket.resetLatch();

            try (Session session = createClientSession("ws3")) {
                ServerSocket.openLatch.await();
            }

            ServerSocket.assertInstances(1);
        }
    }

    @ServerEndpoint(value = "/ws3")
    public static class ServerSocket {

        static Map<ServerSocket, Integer> instanceMap = new ConcurrentHashMap<>();
        static CountDownLatch openLatch;

        static void reset() {
            instanceMap.clear();
            openLatch = null;
        }

        static void resetLatch() {
            openLatch = new CountDownLatch(1);
        }

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
}
