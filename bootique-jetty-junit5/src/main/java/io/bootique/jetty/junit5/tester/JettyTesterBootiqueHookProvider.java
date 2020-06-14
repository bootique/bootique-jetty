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
package io.bootique.jetty.junit5.tester;

import io.bootique.jetty.server.ConnectorHolder;
import io.bootique.jetty.server.ServerHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @since 2.0
 */
public class JettyTesterBootiqueHookProvider implements Provider<JettyTesterBootiqueHook> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyTesterBootiqueHookProvider.class);

    @Inject
    ServerHolder serverHolder;

    private JettyTesterBootiqueHook instance;

    public JettyTesterBootiqueHookProvider(JettyTesterBootiqueHook instance) {
        this.instance = instance;
    }

    @Override
    public JettyTesterBootiqueHook get() {
        assertNotNull(serverHolder, "ServerHolder is not initialized");
        instance.init(serverHolder.getContext(), getConnectorHolder());
        return instance;
    }

    protected ConnectorHolder getConnectorHolder() {
        switch (serverHolder.getConnectorsCount()) {
            case 0:
                throw new IllegalStateException("Can't connect to the application. It has no Jetty connectors configured");
            case 1:
                return serverHolder.getConnector();
            default:
                ConnectorHolder connectorHolder = serverHolder.getConnectors().findFirst().get();
                LOGGER.warn("Application has multiple Jetty connectors. Configuring a client for the first one on port '{}'", connectorHolder.getPort());
                return connectorHolder;
        }
    }
}
