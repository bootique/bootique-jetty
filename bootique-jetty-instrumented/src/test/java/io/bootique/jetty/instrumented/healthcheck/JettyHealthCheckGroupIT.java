package io.bootique.jetty.instrumented.healthcheck;

import io.bootique.BQRuntime;
import io.bootique.jetty.instrumented.unit.InstrumentedJettyApp;
import io.bootique.metrics.health.HealthCheckOutcome;
import io.bootique.metrics.health.HealthCheckRegistry;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class JettyHealthCheckGroupIT {

    @Rule
    public InstrumentedJettyApp app = new InstrumentedJettyApp();

    @Test
    public void testChecksLoaded() {
        BQRuntime runtime = app.start();

        HealthCheckRegistry registry = runtime.getInstance(HealthCheckRegistry.class);
        Map<String, HealthCheckOutcome> results = registry.runHealthChecks();

        assertTrue(results.containsKey(JettyHealthCheckGroupFactory.THREAD_POOL_UTILIZATION_CHECK));
        assertTrue(results.containsKey(JettyHealthCheckGroupFactory.QUEUED_REQUESTS_CHECK));
    }
}
