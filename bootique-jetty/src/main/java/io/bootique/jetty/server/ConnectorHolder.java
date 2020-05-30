/**
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * “License”); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jetty.server;

import org.eclipse.jetty.server.NetworkConnector;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @since 2.0
 */
public class ConnectorHolder {

    private NetworkConnector connector;

    public ConnectorHolder(NetworkConnector connector) {
        this.connector = connector;
    }

    public String getUrl(String context) {

        String protocol = getProtocol();
        String host = getHost();
        int port = resolveNonDefaultPort(protocol);

        StringBuilder url = new StringBuilder(protocol).append("://").append(host);

        if (port > 0) {
            url.append(":").append(port);
        }

        url.append(context);

        return url.toString();
    }

    public int getPort() {
        return connector.getPort();
    }

    public String getHost() {
        if (connector.getHost() != null) {
            return connector.getHost();
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error getting localhost", e);
        }
    }

    public String getProtocol() {

        for (String protocol : connector.getProtocols()) {
            if ("ssl".equals(protocol)) {
                return "https";
            }
        }

        return "http";
    }

    private int resolveNonDefaultPort(String protocol) {
        int port = connector.getPort();
        if (port == 80 && "http".equals(protocol)) {
            return 0;
        }

        if (port == 443 && "https".equals(protocol)) {
            return 0;
        }

        return port;
    }


}
