package io.bootique.jetty.connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SslConnectionFactory;

/**
 * @since 0.18
 */
@JsonTypeName("https")
public class TlsConnectorFactory extends ConnectorFactory {

    @Override
    protected ConnectionFactory buildHttpConnectionFactory(HttpConfiguration httpConfig) {
        SslConnectionFactory factory = new SslConnectionFactory();
        return factory;
    }
}
