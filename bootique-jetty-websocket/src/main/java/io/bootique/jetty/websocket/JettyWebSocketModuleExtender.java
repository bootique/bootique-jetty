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

import io.bootique.ModuleExtender;
import io.bootique.di.Binder;
import io.bootique.di.Key;
import io.bootique.di.SetBuilder;

/**
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class JettyWebSocketModuleExtender extends ModuleExtender<JettyWebSocketModuleExtender> {

    // see JettyWebSocketContextHandlerExtender for the explanation why endpoints are collected as Keys.
    // TL;DR - we need to instantiate them per peer connection unless a user decides they should be singletons...
    private SetBuilder<EndpointKeyHolder> endpointKeys;

    public JettyWebSocketModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public JettyWebSocketModuleExtender initAllExtensions() {
        contributeEndpointKeys();
        return this;
    }

    /**
     * Registers a DI key to locate the endpoint when it is requested by the WebSocket server.
     *
     * @param endpointDiKey a DI key to locate the endpoint.
     * @return this extender instance
     */
    public JettyWebSocketModuleExtender addEndpoint(Key<?> endpointDiKey) {
        contributeEndpointKeys().addInstance(new EndpointKeyHolder(endpointDiKey));
        return this;
    }

    /**
     * Registers a type of endpoint with the WebSocket server.
     *
     * @param endpointType endpoint type used as a DI Key to locate the endpoint in runtime.
     * @return this extender instance
     */
    public JettyWebSocketModuleExtender addEndpoint(Class<?> endpointType) {
        return addEndpoint(Key.get(endpointType));
    }

    protected SetBuilder<EndpointKeyHolder> contributeEndpointKeys() {
        if (endpointKeys == null) {
            endpointKeys = newSet(EndpointKeyHolder.class);
        }
        return endpointKeys;
    }
}
