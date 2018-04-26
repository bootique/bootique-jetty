package io.bootique.jetty.instrumented.server;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.bootique.BQRuntime;
import io.bootique.jetty.instrumented.unit.AssertExtras;
import io.bootique.jetty.instrumented.unit.ThreadPoolTester;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ThreadPoolMetricsIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testUtilization_1() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(1)
                .unblockAfterInProgressRequests(1)
                .afterStartup(r -> checkUtilization(r, 0))
                .afterRequestsFrozen(r -> checkUtilization(r, 1))
                .run("classpath:threads20.yml");
    }

    @Test
    public void testUtilization_3() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(3)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> checkUtilization(r, 0))
                .afterRequestsFrozen(r -> checkUtilization(r, 3))
                .run("classpath:threads20.yml");
    }

    @Test
    public void testQueuedRequests_1() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(4)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> checkQueued(r, 0))
                .afterRequestsFrozen(r -> checkQueued(r, 1))
                .run("classpath:threads8.yml");
    }

    @Test
    public void testQueuedRequests_4() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(7)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> checkQueued(r, 0))
                .afterRequestsFrozen(r -> checkQueued(r, 4))
                .run("classpath:threads8.yml");
    }

    private void checkQueued(BQRuntime runtime, int frozenRequests) {
        Gauge<Integer> gauge = findQueuedRequestsGauge(runtime);
        AssertExtras.assertWithRetry(() -> assertEquals(Integer.valueOf(frozenRequests), gauge.getValue()));
    }

    private void checkUtilization(BQRuntime runtime, int frozenRequests) {
        Gauge<Double> gauge = findUtilizationGauge(runtime);

        // utilization = (acceptorTh + selectorTh + active) / max
        // see more detailed explanation in InstrumentedQueuedThreadPool

        final int acceptors = 2;
        final int selectors = 3;

        // note that Jetty since 9.4 has a number of internal tasks that block threads, so getting an exact utilization
        // number predictably is not possible... So using a huge delta so that plus or minus a busy thread does not
        // fail the test.

        AssertExtras.assertWithRetry(() -> {
            double v = gauge.getValue();
            assertEquals("Actual frozen: " + (int) (v * 20d - acceptors - selectors) + " vs expected " + frozenRequests,
                    (acceptors + selectors + frozenRequests) / 20d, v, 0.1);
        });
    }

    private Stream<Thread> allThreads() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null) {
            tg = tg.getParent();
        }

        Thread[] active = new Thread[tg.activeCount()];
        tg.enumerate(active);
        return Arrays.stream(active);
    }

    private Gauge<Double> findUtilizationGauge(BQRuntime runtime) {
        return findGauge(runtime, InstrumentedQueuedThreadPool.utilizationMetric());
    }

    private Gauge<Integer> findQueuedRequestsGauge(BQRuntime runtime) {
        return findGauge(runtime, InstrumentedQueuedThreadPool.queuedRequestsMetric());
    }

    private <T> Gauge<T> findGauge(BQRuntime runtime, String metricName) {

        MetricRegistry registry = runtime.getInstance(MetricRegistry.class);
        Collection<Gauge> gauges = registry.getGauges((n, m) -> metricName.equals(n)).values();
        assertEquals("Unexpected number of gauges for " + metricName, 1, gauges.size());
        return gauges.iterator().next();
    }
}
