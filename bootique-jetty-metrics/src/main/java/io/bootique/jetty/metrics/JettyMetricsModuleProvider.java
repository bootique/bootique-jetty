package io.bootique.jetty.metrics;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.jetty.JettyModuleProvider;
import io.bootique.metrics.health.HealthCheckModuleProvider;

import java.util.Collection;

import static java.util.Arrays.asList;

/**
 * @since 0.8
 */
public class JettyMetricsModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new JettyMetricsModule();
    }

    @Override
    public Collection<BQModuleProvider> dependencies() {
        return asList(
                new HealthCheckModuleProvider(),
                new JettyModuleProvider()
        );
    }
}
