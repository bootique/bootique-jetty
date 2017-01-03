package io.bootique.jetty.connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;

@BQConfig
@JsonTypeName("http")
public class HttpConnectorFactory extends ConnectorFactory {

    protected ConnectionFactory[] buildHttpConnectionFactories(HttpConfiguration httpConfig) {
        return new ConnectionFactory[]{new HttpConnectionFactory(httpConfig)};
    }
}
