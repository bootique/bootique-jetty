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

/**
 * Creates and configures JSR-356 {@link javax.websocket.server.ServerContainer} within the Jetty environment.
 *
 * @since 1.0.RC1
 */
public class JettyWebSocketContextHandlerExtender implements ServletContextHandlerExtender {

    @Override
    public ServletContextHandler onHandlerCreated(ServletContextHandler handler) {

        try {
            ServerContainer container = WebSocketServerContainerInitializer.configureContext(handler);
        } catch (ServletException e) {
            throw new RuntimeException("Error initializing WebSocket Jetty extensions", e);
        }

        return handler;
    }
}
