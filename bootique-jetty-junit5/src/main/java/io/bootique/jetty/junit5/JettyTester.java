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
package io.bootique.jetty.junit5;

import io.bootique.BQRuntime;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

/**
 * A helper class that is declared in a unit test, manages test Jetty configuration and provides the test with access
 * to the HTTP client. It disables all the app connectors, and binds its own connector on a dynamically-determined
 * port, so that there are no port conflicts.
 *
 * @since 2.0
 */
public class JettyTester {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyTester.class);

    public static JettyTester create() {
        return new JettyTester();
    }

    protected JettyTester() {
    }

    /**
     * Returns an HTTP client pointing to the root of the test Jetty server.
     *
     * @param app test Jetty server app
     * @return a WebTarget to access the test Jetty server.
     */
    public WebTarget getClient(BQRuntime app) {
        Connector[] connectors = app.getInstance(Server.class).getConnectors();

        switch (connectors.length) {
            case 0:
                throw new IllegalStateException("Application has no Jetty connectors configured");
            case 1:
                return getClient(connectors[0]);
            default:
                LOGGER.warn("Application has multiple Jetty connectors. Returning the client for the first one");
                return getClient(connectors[0]);
        }
    }

    protected WebTarget getClient(Connector connector) {

        if (!(connector instanceof ServerConnector)) {
            throw new IllegalStateException("Jetty connector is not a ServerConnector: " + connector);
        }

        ServerConnector serverConnector = (ServerConnector) connector;
        String url = baseUrl(serverConnector);
        return ClientBuilder.newClient().target(url);
    }

    protected String baseUrl(ServerConnector connector) {
        String host = connector.getHost() != null ? connector.getHost() : "localhost";
        int port = connector.getPort();
        String protocol = "http"; // TODO: get protocol from the connector
        String context = "/"; // TODO: non-default context
        return protocol + "://" + host + ":" + port + context;
    }

    public BQModule registerTestHooks() {
        return this::configure;
    }

    protected void configure(Binder binder) {
        throw new UnsupportedOperationException("TODO");
    }
}
