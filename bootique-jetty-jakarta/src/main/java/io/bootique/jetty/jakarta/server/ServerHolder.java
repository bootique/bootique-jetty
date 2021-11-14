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
package io.bootique.jetty.jakarta.server;

import org.eclipse.jetty.server.Server;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Holds Jetty {@link org.eclipse.jetty.server.Server} with additional metadata information. Used by various extensions
 * to extract server URLs.
 *
 * @since 2.0
 */
public class ServerHolder {

    private Server server;
    private String context;
    private Collection<ConnectorHolder> connectors;

    public ServerHolder(Server server, String context, Collection<ConnectorHolder> connectors) {
        this.server = server;
        this.context = context;
        this.connectors = connectors;
    }

    public void stop() throws Exception {
        server.stop();
    }

    public Server getServer() {
        return server;
    }

    public String getContext() {
        return context;
    }

    public int getConnectorsCount() {
        return connectors.size();
    }

    /**
     * Returns a URL of the service assuming there is a single connector. If connector count is not equal to one, an
     * exception is thrown.
     */
    public String getUrl() {
        return getConnector().getUrl(context);
    }

    /**
     * Returns the single {@link ConnectorHolder}. If connector count is not equal to one, an exception is thrown.
     */
    public ConnectorHolder getConnector() {
        if (getConnectorsCount() != 1) {
            throw new IllegalStateException("'getConnector' can be called when there is one and only one connector. " +
                    "Instead there are " + getConnectorsCount() + " connectors");
        }

        return connectors.iterator().next();
    }

    public Stream<ConnectorHolder> getConnectors() {
        return connectors.stream();
    }

    public Stream<String> getUrls() {
        return getConnectors().map(cd -> cd.getUrl(context));
    }
}
