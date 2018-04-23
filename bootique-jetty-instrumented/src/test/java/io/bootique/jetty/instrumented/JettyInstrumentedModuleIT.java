package io.bootique.jetty.instrumented;

import com.codahale.metrics.MetricRegistry;
import io.bootique.BQRuntime;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class JettyInstrumentedModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testMetrics() {
        BQRuntime runtime = testFactory
                .app("--server")
                .autoLoadModules()
                .createRuntime();

        runtime.run();

        MetricRegistry metricRegistry = runtime.getInstance(MetricRegistry.class);

        Set<String> expectedTimers = new HashSet<>(asList("bq.Jetty.Request.Time"));
        assertEquals(expectedTimers, metricRegistry.getTimers().keySet());

        Set<String> expectedGauges = new HashSet<>(asList(
                "bq.Jetty.ThreadPool.Size",
                "bq.Jetty.ThreadPool.QueuedRequests",
                "bq.Jetty.ThreadPool.Utilization"));

        assertEquals(expectedGauges, metricRegistry.getGauges().keySet());
    }
}
