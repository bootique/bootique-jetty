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

import io.bootique.BQCoreModule;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.jetty.junit5.tester.JettyTesterBootiqueHook;
import io.bootique.jetty.junit5.tester.JettyTesterBootiqueHookProvider;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * A helper class that is declared in a unit test, manages test Jetty configuration and provides the test with access
 * to the HTTP client. It disables all the app connectors, and binds its own connector on a dynamically-determined
 * port, so that there are no port conflicts.
 *
 * @since 2.0
 */
public class JettyTester {

    private JettyTesterBootiqueHook bootiqueHook;
    private String serverUrl;

    protected JettyTester() {
        this.bootiqueHook = new JettyTesterBootiqueHook();
    }

    public static JettyTester create() {
        return new JettyTester();
    }

    /**
     * Returns a "target" with URL pointing to the root of the test Jetty server. It can be used to send HTTP
     * requests to the server.
     *
     * @return a WebTarget to access the test Jetty server.
     */
    public WebTarget getTarget() {
        return getTarget(getUrl());
    }

    public String getUrl() {
        if (serverUrl == null) {
            serverUrl = findUrl();
        }

        return serverUrl;
    }

    public int getPort() {
        return bootiqueHook.getConnectorHolder().getPort();
    }

    protected String findUrl() {
        return bootiqueHook.getUrl();
    }

    protected static WebTarget getTarget(String url) {
        return ClientBuilder.newClient().target(url);
    }

    /**
     * Returns a module that replaces all preconfigured Jetty connectors with a single test connector. The test connector
     * will listen on an arbitrary port and will use default settings for the protocol (unencrypted HTTP), header size
     * limits, etc.
     */
    public BQModule moduleReplacingConnectors() {
        return this::configure;
    }

    /**
     * Returns a {@link ResponseMatcher} that helps to assert the response state.
     */
    public static ResponseMatcher matcher(Response response) {
        return new ResponseMatcher(response);
    }

    public static ResponseMatcher assertStatus(Response response, int expectedStatus) {
        return matcher(response).assertStatus(expectedStatus);
    }

    public static ResponseMatcher assertOk(Response response) {
        return matcher(response).assertOk();
    }

    public static ResponseMatcher assertNotFound(Response response) {
        return matcher(response).assertNotFound();
    }

    protected void configure(Binder binder) {
        binder.bind(JettyTesterBootiqueHook.class)
                // wrapping the hook in provider to be able to run the checks for when this tester is erroneously
                // used for multiple runtimes
                .toProviderInstance(new JettyTesterBootiqueHookProvider(bootiqueHook))
                // using "initOnStartup" to ensure the hook is resolved before the test start. Any downsides?
                .initOnStartup();

        BQCoreModule.extend(binder).addPostConfig("classpath:io/bootique/jetty/junit5/JettyTester.yml");
    }
}
