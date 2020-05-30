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
package io.bootique.jetty.connector;

/**
 * Encapsulates either an explicit or a dynamic network port.
 *
 * @since 2.0
 */
public class PortFactory {

    private static final String ANY_PORT_LABEL = "any";
    static final int DEFAULT_PORT = 8080;

    private String portString;
    private int port;

    public PortFactory(int port) {

        if (port <= 0) {
            throw new IllegalArgumentException("Port must be a positive integer: " + portString);
        }

        this.port = port;
    }

    /**
     * @param portString can be either null, empty string, a positive integer or a keyword "dynamic".
     */
    public PortFactory(String portString) {
        this.portString = portString;
    }

    public int resolve(String host) {

        if (port > 0) {
            return port;
        } else if (portString == null || portString.isEmpty()) {
            return DEFAULT_PORT;
        } else if (ANY_PORT_LABEL.equals(portString)) {
            return findAvailablePort(host);
        } else {
            return resolveExplicitPort();
        }
    }

    protected int findAvailablePort(String host) {
        return PortFinder.findAvailablePort(host);
    }

    protected int resolveExplicitPort() {
        int port;
        try {
            port = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port. Must be either a positive integer or a keyword 'dynamic': " + portString);
        }

        if (port <= 0) {
            throw new IllegalArgumentException("Port must be a positive integer: " + portString);
        }

        return port;
    }
}
