package io.bootique.jetty.connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.SslConnectionFactory;

/**
 * @since 0.18
 */
@JsonTypeName("https")
public class TlsConnectorFactory extends ConnectorFactory {

    @Override
    protected ConnectionFactory[] buildHttpConnectionFactories(HttpConfiguration httpConfig) {
        return new ConnectionFactory[]{
                new SslConnectionFactory(), new HttpConnectionFactory()
        };
    }

    @Override
    protected HttpConfiguration buildHttpConfiguration() {
        HttpConfiguration config = super.buildHttpConfiguration();
        config.addCustomizer(new SecureRequestCustomizer());
        return config;
    }
}
