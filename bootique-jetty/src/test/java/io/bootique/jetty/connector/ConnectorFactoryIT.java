package io.bootique.jetty.connector;

import io.bootique.test.junit.PolymorphicConfigurationChecker;
import org.junit.Test;

public class ConnectorFactoryIT {

    @Test
    public void testPolymorphicConfiguration() {
        PolymorphicConfigurationChecker
                .test(ConnectorFactory.class, HttpConnectorFactory.class, HttpsConnectorFactory.class);
    }
}
