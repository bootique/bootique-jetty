package io.bootique.jetty.server;

import org.eclipse.jetty.server.NetworkConnector;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @since 0.18
 */
public class ConnectorDescriptor {

    private NetworkConnector connector;

    public ConnectorDescriptor(NetworkConnector connector) {
        this.connector = connector;
    }

    public String getUrl(String context) {

        String protocol = getProtocol();
        String host = getHost();
        int port = getPort(protocol);

        StringBuilder url = new StringBuilder(protocol).append("://").append(host);

        if (port > 0) {
            url.append(":").append(port);
        }

        url.append(context);

        return url.toString();
    }

    private String getProtocol() {

        for (String protocol : connector.getProtocols()) {
            if ("ssl".equals(protocol)) {
                return "https";
            }
        }

        return "http";
    }

    private int getPort(String protocol) {
        int port = connector.getPort();
        if (port == 80 && "http".equals(protocol)) {
            return 0;
        }

        if (port == 443 && "https".equals(protocol)) {
            return 0;
        }

        return port;
    }

    private String getHost() {
        if (connector.getHost() != null) {
            return connector.getHost();
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error getting localhost", e);
        }
    }
}
