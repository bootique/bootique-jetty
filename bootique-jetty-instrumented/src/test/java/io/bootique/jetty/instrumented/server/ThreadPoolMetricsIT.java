package io.bootique.jetty.instrumented.server;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.bootique.BQRuntime;
import io.bootique.jetty.instrumented.unit.InstrumentedJettyApp;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ThreadPoolMetricsIT {

    @Rule
    public InstrumentedJettyApp app = new InstrumentedJettyApp();

    @Test
    public void testUtilizationVsMax_2() throws InterruptedException {

        new ThreadPoolTester(app)
                .startRequests(2)
                .unblockAfter(2)
                .checkAfterStartup(this::checkUtilizationVsMax_OnStartup)
                .checkWithRequestsFrozen(r -> checkUtilizationVsMax_WithRequestsFrozen(r, 2))
                .run("classpath:threads20.yml");
    }

    @Test
    public void testUtilizationVsMax_1() throws InterruptedException {

        new ThreadPoolTester(app)
                .startRequests(1)
                .unblockAfter(1)
                .checkAfterStartup(this::checkUtilizationVsMax_OnStartup)
                .checkWithRequestsFrozen(r -> checkUtilizationVsMax_WithRequestsFrozen(r, 1))
                .run("classpath:threads20.yml");
    }

    @Test
    public void testQueuedRequests_1() throws InterruptedException {

        new ThreadPoolTester(app)
                .startRequests(4)
                .unblockAfter(3)
                .checkAfterStartup(this::checkQueuedRequests_OnStartup)
                .checkWithRequestsFrozen(r -> checkQueuedRequests_WithRequestsFrozen(r, 1))
                .run("classpath:threads7.yml");
    }

    @Test
    public void testQueuedRequests_4() throws InterruptedException {

        new ThreadPoolTester(app)
                .startRequests(7)
                .unblockAfter(3)
                .checkAfterStartup(this::checkQueuedRequests_OnStartup)
                .checkWithRequestsFrozen(r -> checkQueuedRequests_WithRequestsFrozen(r, 4))
                .run("classpath:threads7.yml");
    }

    private void checkQueuedRequests_OnStartup(BQRuntime runtime) {
        Gauge<Integer> gauge = findQueuedRequestsGauge(runtime);
        assertEquals(Integer.valueOf(0), gauge.getValue());
    }

    private void checkQueuedRequests_WithRequestsFrozen(BQRuntime runtime, int frozenRequests) {
        Gauge<Integer> gauge = findQueuedRequestsGauge(runtime);
        assertEquals(Integer.valueOf(frozenRequests), gauge.getValue());
    }

    private void checkUtilizationVsMax_OnStartup(BQRuntime runtime) {
        Gauge<Double> gauge = findUtilizationVsMaxGauge(runtime);

        // utilizationMax = (acceptorTh + selectorTh + active) / max
        // see more detailed explanation in InstrumentedQueuedThreadPool
        assertEquals((2 + 3 + 0) / 20d, gauge.getValue(), 0.0001);
    }

    private void checkUtilizationVsMax_WithRequestsFrozen(BQRuntime runtime, int frozenRequests) {
        Gauge<Double> gauge = findUtilizationVsMaxGauge(runtime);

        // debug race conditions (we saw some on Travis)
        dumpJettyThreads();

        // utilizationMax = (acceptorTh + selectorTh + active) / max
        // see more detailed explanation in InstrumentedQueuedThreadPool
        assertEquals((2 + 3 + frozenRequests) / 20d, gauge.getValue(), 0.0001);
    }

    private void dumpJettyThreads() {

        // debugging travis failures...
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null) {
            tg = tg.getParent();
        }

        Thread[] active = new Thread[tg.activeCount()];
        tg.enumerate(active);

        // there is a small chance a thread becomes inactive between 'activeCount' and 'enumerate' calls,
        // resulting in null threads in the array.. remove null threads from the result
        Arrays.stream(active).filter(t -> t != null && t.getName()
                .startsWith("bootique-http"))
                .map(t -> t.getName() + " - " + t.getState())
                .forEach(System.out::println);
    }

    private Gauge<Double> findUtilizationVsMaxGauge(BQRuntime runtime) {
        return findGauge(Double.class, runtime, "utilization-max");
    }

    private Gauge<Integer> findQueuedRequestsGauge(BQRuntime runtime) {
        return findGauge(Integer.class, runtime, "queued-requests");
    }

    private <T> Gauge<T> findGauge(Class<T> type, BQRuntime runtime, String label) {

        MetricRegistry registry = runtime.getInstance(MetricRegistry.class);
        String name = MetricRegistry.name(QueuedThreadPool.class, "bootique-http", label);

        Collection<Gauge> gauges = registry.getGauges((n, m) -> name.equals(n)).values();
        assertEquals("Unexpected number of gauges for " + name, 1, gauges.size());
        return gauges.iterator().next();
    }
}
