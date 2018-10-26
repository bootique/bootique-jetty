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

import io.bootique.jetty.server.ServletContextHandlerExtender;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Set;

/**
 * Creates and configures JSR-356 {@link javax.websocket.server.ServerContainer} within the Jetty environment.
 *
 * @since 1.0.RC1
 */
public class JettyWebSocketContextHandlerExtender implements ServletContextHandlerExtender {

    private Set<Object> endpoints;

    public JettyWebSocketContextHandlerExtender(Set<Object> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public void onHandlerInstalled(ServletContextHandler handler) {

        ServerContainer wsContainer;

        try {
            // install WebSocket extensions..
            wsContainer = WebSocketServerContainerInitializer.configureContext(handler);
        } catch (ServletException e) {
            throw new RuntimeException("Error initializing WebSocket Jetty extensions", e);
        }

        endpoints.forEach(e -> installEndpoint(e, wsContainer));
    }

    protected void installEndpoint(Object endpoint, ServerContainer wsContainer) {

        Class<?> endpointType = endpoint.getClass();
        ServerEndpoint endpointAnnotation = endpointType.getAnnotation(ServerEndpoint.class);
        if (endpointAnnotation == null) {
            // TODO: handle subclasses of javax.websocket.Endpoint
            throw new IllegalArgumentException(endpointType.getName() + " is not annotated with @ServerEndpoint");
        }

        String path = normalizePath(endpointAnnotation.value());

        // TODO: other pieces provided by @ServerEndpoint
        ServerEndpointConfig config = ServerEndpointConfig.Builder
                .create(endpointType, path)
                // make sure Bootique-configured instances are used as endpoints...
                .configurator(new BQEndpointConfiguration(endpoint))
                .build();

        try {
            wsContainer.addEndpoint(config);
        } catch (DeploymentException e) {
            throw new RuntimeException("Error installing endpoint " + endpointType.getName(), e);
        }
    }

    protected String normalizePath(String path) {

        if(path == null || path.isEmpty()) {
            return "/";
        }

        if(!path.startsWith("/")) {
            return "/" + path;
        }

        return path;
    }

    class BQEndpointConfiguration extends ServerEndpointConfig.Configurator {
        private Object instance;

        public BQEndpointConfiguration(Object instance) {
            this.instance = instance;
        }

        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) {
            return (T) instance;
        }
    }
}
