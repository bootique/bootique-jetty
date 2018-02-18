package io.bootique.jetty.instrumented;

import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.metrics.MetricsModule;
import io.bootique.metrics.health.HealthCheckModule;
import io.bootique.test.junit.BQModuleProviderChecker;
import io.bootique.test.junit.BQRuntimeChecker;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

public class InstrumentedJettyModuleProviderTest {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testPresentInJar() {
        BQModuleProviderChecker.testPresentInJar(InstrumentedJettyModuleProvider.class);
    }

    @Test
    public void testModuleDeclaresDependencies() {
        final BQRuntime bqRuntime = testFactory.app().module(new InstrumentedJettyModuleProvider()).createRuntime();
        BQRuntimeChecker.testModulesLoaded(bqRuntime,
                JettyModule.class,
                HealthCheckModule.class,
                MetricsModule.class
        );
    }
}
