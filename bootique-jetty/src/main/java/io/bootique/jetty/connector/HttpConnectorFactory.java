package io.bootique.jetty.connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;

@JsonTypeName("http")
public class HttpConnectorFactory extends ConnectorFactory {

    protected HttpConnectionFactory buildHttpConnectionFactory(HttpConfiguration httpConfig) {
        return new HttpConnectionFactory(httpConfig);
    }
}
