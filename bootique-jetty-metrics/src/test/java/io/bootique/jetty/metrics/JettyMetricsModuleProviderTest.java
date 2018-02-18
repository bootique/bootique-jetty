package io.bootique.jetty.metrics;

import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.metrics.health.HealthCheckModule;
import io.bootique.test.junit.BQModuleProviderChecker;
import io.bootique.test.junit.BQRuntimeChecker;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

public class JettyMetricsModuleProviderTest {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testAutoLoadable() {
        BQModuleProviderChecker.testAutoLoadable(JettyMetricsModuleProvider.class);
    }

    @Test
    public void testModuleDeclaresDependencies() {
        final BQRuntime bqRuntime = testFactory.app().module(new JettyMetricsModuleProvider()).createRuntime();
        BQRuntimeChecker.testModulesLoaded(bqRuntime,
                JettyModule.class,
                HealthCheckModule.class
        );
    }
}
