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
import org.junit.Test;

import javax.inject.Inject;
import javax.websocket.DeploymentException;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class JettyWebSocketModuleInjectionIT extends JettyWebSocketTestBase {

    @Test
    public void testInjectionInEndpoint() throws IOException, DeploymentException, InterruptedException {

        EchoService service = new EchoService("echo");

        BQRuntime runtime = testFactory.app("-s")
                .autoLoadModules()
                .module(b -> b.bind(EchoService.class).toInstance(service))
                .module(b -> b.bind(ServerSocket.class).inSingletonScope())
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(ServerSocket.class))
                .createRuntime();

        runtime.run();

        ServerSocket serverSocket = runtime.getInstance(ServerSocket.class);
        serverSocket.assertBuffer("");

        try (Session session = createClientSession("ws")) {

            session.getBasicRemote().sendText("hello");
            serverSocket.messageLatch.await();
            serverSocket.assertBuffer("echo[hello]");
        }
    }

    public static class EchoService {

        private String prefix;

        EchoService(String prefix) {
            this.prefix = prefix;
        }

        public String echo(String message) {
            return prefix + "[" + message + "]";
        }
    }

    @ServerEndpoint(value = "/ws")
    public static class ServerSocket {

        @Inject
        EchoService echoService;

        CountDownLatch messageLatch = new CountDownLatch(1);
        StringBuilder buffer = new StringBuilder();

        void assertBuffer(String expected) {
            assertEquals(expected, buffer.toString());
        }

        @OnMessage
        public void onMessageText(String message) {
            buffer.append(echoService.echo(message));
            messageLatch.countDown();
        }

        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }

}
