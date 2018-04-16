package io.bootique.jetty.instrumented.healthcheck;

import io.bootique.BQRuntime;
import io.bootique.metrics.health.HealthCheckOutcome;
import io.bootique.metrics.health.HealthCheckRegistry;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class JettyHealthCheckGroupIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testChecksLoaded() {
        BQRuntime runtime = testFactory.app("-s").createRuntime();
        runtime.run();

        HealthCheckRegistry registry = runtime.getInstance(HealthCheckRegistry.class);
        Map<String, HealthCheckOutcome> results = registry.runHealthChecks();

        assertTrue(results.containsKey(JettyHealthCheckGroupFactory.POOL_UTILIZATION_CHECK));
        assertTrue(results.containsKey(JettyHealthCheckGroupFactory.QUEUED_REQUESTS_CHECK));
    }
}
