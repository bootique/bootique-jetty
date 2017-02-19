package io.bootique.jetty.metrics;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class JettyMetricsModuleProviderTest {

    @Test
    public void testAutoLoad() {
        BQModuleProviderChecker.testPresentInJar(JettyMetricsModuleProvider.class);
    }
}
