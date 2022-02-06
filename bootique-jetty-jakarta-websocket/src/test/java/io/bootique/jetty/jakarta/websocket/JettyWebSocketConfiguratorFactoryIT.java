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

import io.bootique.BQRuntime;
import io.bootique.jetty.jakarta.servlet.ServletEnvironment;
import jakarta.websocket.server.ServerContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JettyWebSocketConfiguratorFactoryIT extends JettyWebSocketTestBase {

    @Test
    public void testCustomPolicy() {

        BQRuntime runtime = testFactory.app("-s", "-c", "classpath:io/bootique/jetty/jakarta/websocket/WebSocketPolicyFactoryIT.yml")
                .module(jetty.moduleReplacingConnectors())
                .createRuntime();

        runtime.run();

        ServerContainer container = (ServerContainer) runtime.getInstance(ServletEnvironment.class)
                .context()
                .get()
                .getAttribute(ServerContainer.class.getName());

        assertNotNull(container);

        // assert policy was applied properly
        assertEquals(5 * 1000L, container.getDefaultAsyncSendTimeout());
        assertEquals(30 * 60 * 1000L, container.getDefaultMaxSessionIdleTimeout());
        assertEquals(30 * 1024, container.getDefaultMaxBinaryMessageBufferSize());
        assertEquals(45 * 1024, container.getDefaultMaxTextMessageBufferSize());
    }
}
