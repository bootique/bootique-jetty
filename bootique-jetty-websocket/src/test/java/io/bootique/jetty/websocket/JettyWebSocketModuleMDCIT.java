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

import io.bootique.jetty.JettyModule;
import io.bootique.jetty.request.RequestMDCItem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JettyWebSocketModuleMDCIT extends JettyWebSocketTestBase {

    @Test
    public void contextPassed() throws IOException, DeploymentException, InterruptedException {

        testFactory.app("-s")
                .module(jetty.moduleReplacingConnectors())
                .module(b -> JettyModule.extend(b).addRequestMDCItem("x", new TestMDCItem()))
                .module(b -> JettyWebSocketModule.extend(b).addEndpoint(WsServer.class))
                .run();

        assertNull(WsServer.onOpen);
        assertNull(WsServer.onMessage);
        assertNull(WsServer.onClose);

        try (Session s = createClientSession("?x=aa")) {

            Thread.sleep(500);
            assertEquals("x:aa", WsServer.onOpen);

            assertNull(WsServer.onMessage);
            assertNull(WsServer.onClose);

            // important to test sending a message, as it will likely happen in a thread different from the original one
            s.getBasicRemote().sendText("m");

            Thread.sleep(500);
            assertEquals("x:aa", WsServer.onMessage);
            assertNull(WsServer.onClose);
        }

        Thread.sleep(500);
        assertEquals("x:aa", WsServer.onClose);
    }

    static class TestMDCItem implements RequestMDCItem {

        @Override
        public void initMDC(ServletContext sc, ServletRequest request) {
            MDC.put("x", "x:" + request.getParameter("x"));
        }

        @Override
        public void cleanupMDC(ServletContext sc, ServletRequest request) {
            MDC.remove("x");
        }
    }

    @ServerEndpoint("/")
    public static class WsServer {

        static final Logger LOGGER = LoggerFactory.getLogger(WsServer.class);

        static String onOpen;
        static String onMessage;
        static String onClose;

        @Inject
        WebSocketMDCManager mdc;

        @OnOpen
        public void onOpen(Session session) {
            mdc.run(session, () -> {
                LOGGER.info("onOpen");
                onOpen = MDC.get("x");
            });
        }

        @OnMessage
        public void onMessage(String message, Session session) {
            mdc.run(session, () -> {
                LOGGER.info("onMessage");
                onMessage = MDC.get("x");
            });
        }

        @OnClose
        public void onClose(Session session) {
            mdc.run(session, () -> {
                LOGGER.info("onClose");
                onClose = MDC.get("x");
            });
        }

        @OnError
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }
}
