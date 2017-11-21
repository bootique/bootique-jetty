package io.bootique.jetty.instrumented.healthcheck;

import io.bootique.metrics.health.HealthCheck;
import io.bootique.metrics.health.HealthCheckGroup;

import java.util.Map;

/**
 * A container of Jetty-specific health checks.
 *
 * @since 0.25
 */
public class JettyHealthCheckGroup implements HealthCheckGroup {

    private Map<String, HealthCheck> healthChecks;

    public JettyHealthCheckGroup(Map<String, HealthCheck> healthChecks) {
        this.healthChecks = healthChecks;
    }

    @Override
    public Map<String, HealthCheck> getHealthChecks() {
        return healthChecks;
    }
}
