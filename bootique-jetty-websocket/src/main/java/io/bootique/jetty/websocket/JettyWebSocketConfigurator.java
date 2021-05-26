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

import io.bootique.di.Injector;
import io.bootique.di.Key;
import io.bootique.jetty.request.RequestMDCManager;
import io.bootique.jetty.server.ServletContextHandlerExtender;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

/**
 * Creates and configures JSR-356 {@link javax.websocket.server.ServerContainer}, linking Bootique and Jetty environments,
 * providing the ability to look up Endpoints via DI.
 */
public class JettyWebSocketConfigurator implements ServletContextHandlerExtender {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyWebSocketConfigurator.class);

    private final RequestMDCManager mdcManager;
    private final Set<EndpointKeyHolder> endpointDiKeys;
    private final Injector injector;
    private final WebSocketPolicy webSocketPolicy;

    public JettyWebSocketConfigurator(
            Injector injector,
            WebSocketPolicy webSocketPolicy,
            Set<EndpointKeyHolder> endpointDiKeys,
            RequestMDCManager mdcManager) {

        this.injector = injector;
        this.endpointDiKeys = endpointDiKeys;
        this.webSocketPolicy = webSocketPolicy;
        this.mdcManager = mdcManager;
    }

    @Override
    public void onHandlerInstalled(ServletContextHandler handler) {

        ServerContainer wsContainer;

        try {
            // install Jetty WebSocket support...
            wsContainer = WebSocketServerContainerInitializer.configureContext(handler);
        } catch (ServletException e) {
            throw new RuntimeException("Error initializing WebSocket Jetty extensions", e);
        }

        configurePolicy(wsContainer);
        endpointDiKeys.forEach(e -> installEndpoint(e.getKey(), wsContainer));
    }

    protected void configurePolicy(ServerContainer wsContainer) {
        wsContainer.setAsyncSendTimeout(webSocketPolicy.getAsyncWriteTimeout());
        wsContainer.setDefaultMaxSessionIdleTimeout(webSocketPolicy.getIdleTimeout());
        wsContainer.setDefaultMaxBinaryMessageBufferSize(webSocketPolicy.getMaxBinaryMessageBufferSize());
        wsContainer.setDefaultMaxTextMessageBufferSize(webSocketPolicy.getMaxTextMessageBufferSize());
    }

    protected <T> void installEndpoint(Key<T> endpointDiKey, ServerContainer wsContainer) {

        Class<? super T> endpointType = endpointDiKey.getType().getRawType();
        ServerEndpoint endpointAnnotation = endpointType.getAnnotation(ServerEndpoint.class);
        if (endpointAnnotation == null) {
            // TODO: must handle subclasses of javax.websocket.Endpoint
            throw new IllegalArgumentException(endpointType.getName() + " is not annotated with @ServerEndpoint");
        }

        Supplier<T> endpointSupplier = () -> injector.getInstance(endpointDiKey);
        ServerEndpointConfig config = createConfigFromAnnotation(endpointType, endpointSupplier, endpointAnnotation);

        LOGGER.info("Adding WebSocket endpoint mapped to {}", config.getPath());

        try {
            wsContainer.addEndpoint(config);
        } catch (DeploymentException e) {
            throw new RuntimeException("Error installing endpoint " + endpointType.getName(), e);
        }
    }

    private <T> ServerEndpointConfig createConfigFromAnnotation(
            Class<? super T> endpointType,
            Supplier<T> endpointSupplier,
            ServerEndpoint endpointAnnotation) {

        String path = normalizePath(endpointAnnotation.value());

        // Ignore "configurator" annotation value (unless we uncover a valid use case for overriding the one supplied
        // by Bootique below. Honor all other annotation parameters though
        if (endpointAnnotation.configurator() != ServerEndpointConfig.Configurator.class) {
            LOGGER.warn("@ServerEndpoint.configurator is not null, but will be ignored");
        }

        return ServerEndpointConfig.Builder
                .create(endpointType, path)
                // making sure Bootique-configured instances are returned for endpoints...
                .configurator(new BQEndpointConfiguration(endpointSupplier))
                .decoders(asList(endpointAnnotation.decoders()))
                .encoders(asList(endpointAnnotation.encoders()))
                .subprotocols(asList(endpointAnnotation.subprotocols()))
                .build();
    }

    protected String normalizePath(String path) {

        if (path == null || path.isEmpty()) {
            return "/";
        }

        if (!path.startsWith("/")) {
            return "/" + path;
        }

        return path;
    }

    class BQEndpointConfiguration extends ServerEndpointConfig.Configurator {

        // Per JSR-356 endpoints are 1 instance per peer by default, or can be singletons. In any event
        // "The implementation must call this method each time a new client connects to the logical endpoint."

        // A longer quote from the JSR:

        // "The developer may control the creation of endpoint instances by supplying a ServerEndpointConfig.
        // Configurator object that overrides the getEndpointInstance() call. The implementation must call this
        // method each time a new client connects to the logical endpoint. [WSC-3.1.7-1] The platform default
        // implementation of this method is to return a new instance of the endpoint class each time it is called.
        // [WSC-3.1.7-2] In this way, developers may deploy endpoints in such a way that only one instance of the
        // endpoint class is instantiated for all the client connections to the logical endpoints. In this case,
        // developers are cautioned that such a singleton instance of the endpoint class will have to program
        // with concurrent calling threads in mind, for example, if two different clients send a message at the same
        // time."

        private Supplier<?> endpointSupplier;

        BQEndpointConfiguration(Supplier<?> endpointSupplier) {
            this.endpointSupplier = endpointSupplier;
        }

        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) {
            return (T) endpointSupplier.get();
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {

            // clone MDC data into session
            Set<String> keys = mdcManager.mdcKeys();
            if (!keys.isEmpty()) {
                Map<String, String> mdcData = new HashMap<>(3);

                for (String key : keys) {
                    String value = MDC.get(key);
                    if (value != null) {
                        mdcData.put(key, value);
                    }
                }

                if (!mdcData.isEmpty()) {
                    sec.getUserProperties().put(WebSocketMDCManager.MDC_MAP_KEY, mdcData);
                }
            }
        }
    }
}
