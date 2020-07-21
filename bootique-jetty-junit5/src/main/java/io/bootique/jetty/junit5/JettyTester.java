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
import io.bootique.BQRuntime;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.jetty.junit5.tester.JettyConnectorAccessor;
import io.bootique.jetty.junit5.tester.JettyTesterBootiqueHook;
import io.bootique.jetty.junit5.tester.JettyTesterBootiqueHookProvider;
import io.bootique.jetty.server.ServerHolder;

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

    /**
     * Returns an HTTP client WebTarget for the jetty server running within the app passed as a "jettyApp" parameter.
     * Usually a JettyTester instance is associated with a specific app, so the instance's {@link #getTarget()} method is
     * used. This static method is for special occasions when JettyTester is not allowed to replace the app's connector.
     *
     * @param jettyApp a Bootique runtime object that includes a Jetty server
     * @return a WebTarget to access the test Jetty server.
     */
    public static WebTarget getTarget(BQRuntime jettyApp) {
        return getTarget(getUrl(jettyApp));
    }

    /**
     * Returns a URL of the jetty server running within the app passed as a "jettyApp" parameter.
     * Usually a JettyTester instance is associated with a specific app, so the instance's {@link #getUrl()} method is
     * used. This static method is for special occasions when JettyTester is not allowed to replace the app's connector.
     *
     * @param jettyApp a Bootique runtime object that includes a Jetty server
     * @return a URL to access the Jetty server
     */
    public static String getUrl(BQRuntime jettyApp) {
        ServerHolder serverHolder = jettyApp.getInstance(ServerHolder.class);
        return JettyConnectorAccessor.getConnectorHolder(serverHolder).getUrl(serverHolder.getContext());
    }

    protected static WebTarget getTarget(String url) {
        return ClientBuilder.newClient().target(url);
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

    public static ResponseMatcher assertOk(Response response, String message) {
        return matcher(response).assertOk(message);
    }

    public static ResponseMatcher assertCreated(Response response) {
        return matcher(response).assertCreated();
    }

    public static ResponseMatcher assertCreated(Response response, String message) {
        return matcher(response).assertCreated(message);
    }

    public static ResponseMatcher assertBadRequest(Response response) {
        return matcher(response).assertBadRequest();
    }

    public static ResponseMatcher assertBadRequest(Response response, String message) {
        return matcher(response).assertBadRequest(message);
    }

    public static ResponseMatcher assertUnauthorized(Response response) {
        return matcher(response).assertUnauthorized();
    }

    public static ResponseMatcher assertUnauthorized(Response response, String message) {
        return matcher(response).assertUnauthorized(message);
    }

    public static ResponseMatcher assertForbidden(Response response) {
        return matcher(response).assertForbidden();
    }

    public static ResponseMatcher assertForbidden(Response response, String message) {
        return matcher(response).assertForbidden(message);
    }

    public static ResponseMatcher assertNotFound(Response response) {
        return matcher(response).assertNotFound();
    }

    public static ResponseMatcher assertNotFound(Response response, String message) {
        return matcher(response).assertNotFound(message);
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
