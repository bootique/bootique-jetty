package io.bootique.jetty.instrumented;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.JettyModuleProvider;
import io.bootique.metrics.MetricsModuleProvider;
import io.bootique.metrics.health.HealthCheckModuleProvider;

import java.util.Collection;
import java.util.Collections;

import static java.util.Arrays.asList;

/**
 * @since 0.11
 */
public class JettyInstrumentedModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new JettyInstrumentedModule();
    }

    @Override
    public Collection<Class<? extends Module>> overrides() {
        return Collections.singleton(JettyModule.class);
    }

    @Override
    public Collection<BQModuleProvider> dependencies() {
        return asList(
                new MetricsModuleProvider(),
                new HealthCheckModuleProvider(),
                new JettyModuleProvider()
        );
    }
}
