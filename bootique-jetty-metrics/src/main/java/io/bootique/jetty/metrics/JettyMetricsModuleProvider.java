package io.bootique.jetty.metrics;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;

/**
 * @since 0.8
 */
public class JettyMetricsModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new JettyMetricsModule();
    }
}
