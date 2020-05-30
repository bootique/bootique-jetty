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
import io.bootique.jetty.server.ServerHolder;
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
        ServerHolder serverHolder = app.getInstance(ServerHolder.class);

        switch (serverHolder.getConnectorsCount()) {
            case 0:
                throw new IllegalStateException("Can't connect to the application. It has no Jetty connectors configured");
            case 1:
                return getClient(serverHolder.getUrl());
            default:
                String url = serverHolder.getUrls().findFirst().get();
                LOGGER.warn("Application has multiple Jetty connectors. Returning the client for the first one at '{}'", url);
                return getClient(url);
        }
    }

    protected WebTarget getClient(String url) {
        return ClientBuilder.newClient().target(url);
    }

    public BQModule registerTestHooks() {
        return this::configure;
    }

    protected void configure(Binder binder) {
        throw new UnsupportedOperationException("TODO");
    }
}
