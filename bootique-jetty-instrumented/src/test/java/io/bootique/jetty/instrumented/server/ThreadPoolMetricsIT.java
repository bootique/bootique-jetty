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
                .parallelRequests(2)
                .checkAfterStartup(this::checkUtilizationVsMax_OnStartup)
                .checkWithRequestsFrozen(r -> checkUtilizationVsMax_WithRequestsFrozen(r, 2))
                .run("classpath:threads.yml");
    }

    @Test
    public void testUtilizationVsMax_1() throws InterruptedException {

        new ThreadPoolTester(app)
                .parallelRequests(1)
                .checkAfterStartup(this::checkUtilizationVsMax_OnStartup)
                .checkWithRequestsFrozen(r -> checkUtilizationVsMax_WithRequestsFrozen(r, 1))
                .run("classpath:threads.yml");
    }

    private Gauge<Double> findUtilizationVsMaxGauge(BQRuntime runtime) {
        return findGauge(Double.class, runtime, "utilization-max");
    }

    private <T> Gauge<T> findGauge(Class<T> type, BQRuntime runtime, String label) {

        MetricRegistry registry = runtime.getInstance(MetricRegistry.class);
        String name = MetricRegistry.name(QueuedThreadPool.class, "bootique-http", label);

        Collection<Gauge> gauges = registry.getGauges((n, m) -> name.equals(n)).values();
        assertEquals("Unexpected number of gauges for " + name, 1, gauges.size());
        return gauges.iterator().next();
    }

    private void checkUtilizationVsMax_OnStartup(BQRuntime runtime) {
        Gauge<Double> utilizationVsMax = findUtilizationVsMaxGauge(runtime);

        // utilizationMax = (acceptorTh + selectorTh + active) / max
        // see more detailed explanation in InstrumentedQueuedThreadPool
        assertEquals((2 + 3 + 0) / 20d, utilizationVsMax.getValue(), 0.0001);
    }

    private void checkUtilizationVsMax_WithRequestsFrozen(BQRuntime runtime, int frozenRequests) {
        Gauge<Double> utilizationVsMax = findUtilizationVsMaxGauge(runtime);

        // debug race conditions (we saw some on Travis)
        dumpJettyThreads();

        // utilizationMax = (acceptorTh + selectorTh + active) / max
        // see more detailed explanation in InstrumentedQueuedThreadPool
        assertEquals((2 + 3 + frozenRequests) / 20d, utilizationVsMax.getValue(), 0.0001);
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
}
