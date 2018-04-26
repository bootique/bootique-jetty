package io.bootique.jetty.instrumented.server;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import io.bootique.jetty.instrumented.JettyInstrumentedModule;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.concurrent.BlockingQueue;

public class InstrumentedQueuedThreadPool extends QueuedThreadPool {

    // for now assuming a single thread pool, so we can hardcode its name
    private static final String sizeMetric = JettyInstrumentedModule
            .METRIC_NAMING
            .name("ThreadPool", "Size");

    private static final String queuedRequestsMetric = JettyInstrumentedModule
            .METRIC_NAMING
            .name("ThreadPool", "QueuedRequests");

    private static final String utilization = JettyInstrumentedModule
            .METRIC_NAMING
            .name("ThreadPool", "Utilization");

    private MetricRegistry metricRegistry;

    public InstrumentedQueuedThreadPool(MetricRegistry metricRegistry) {
        super();
        this.metricRegistry = metricRegistry;
    }

    public InstrumentedQueuedThreadPool(
            int maxThreads,
            int minThreads,
            int idleTimeout,
            BlockingQueue<Runnable> queue,
            ThreadGroup threadGroup,
            MetricRegistry metricRegistry) {

        super(maxThreads, minThreads, idleTimeout, queue, threadGroup);
        this.metricRegistry = metricRegistry;
    }

    public InstrumentedQueuedThreadPool(
            int maxThreads,
            int minThreads,
            int idleTimeout,
            BlockingQueue<Runnable> queue,
            MetricRegistry metricRegistry) {

        super(maxThreads, minThreads, idleTimeout, queue);
        this.metricRegistry = metricRegistry;
    }

    public InstrumentedQueuedThreadPool(
            int maxThreads,
            int minThreads,
            int idleTimeout,
            MetricRegistry metricRegistry) {

        super(maxThreads, minThreads, idleTimeout);
        this.metricRegistry = metricRegistry;
    }

    public InstrumentedQueuedThreadPool(
            int maxThreads,
            int minThreads,
            MetricRegistry metricRegistry) {

        super(maxThreads, minThreads);
        this.metricRegistry = metricRegistry;
    }

    public InstrumentedQueuedThreadPool(
            int maxThreads,
            MetricRegistry metricRegistry) {

        super(maxThreads);
        this.metricRegistry = metricRegistry;
    }

    public static String sizeMetric() {
        return sizeMetric;
    }

    public static String queuedRequestsMetric() {
        return queuedRequestsMetric;
    }

    public static String utilizationMetric() {
        return utilization;
    }

    @Override
    protected void doStart() throws Exception {

        super.doStart();

        metricRegistry.register(sizeMetric(), (Gauge<Integer>) this::getThreads);
        metricRegistry.register(queuedRequestsMetric(), (Gauge<Integer>) this::getQueueSize);
        metricRegistry.register(utilizationMetric(), (Gauge<Double>) this::getUtilization);
    }

    protected double getUtilization() {

        // utilization is:
        //     (all_acceptor_t + all_selector_t + active_request_t) / maxT

        // This is not readily apparent from the Jetty API below. An explanation:
        //     getThreads()                    == all_acceptor_t + all_selector_t + active_request_t + idle_request_t
        // hence
        //     getThreads() - getIdleThreads() == all_acceptor_t + all_selector_t + active_request_t

        return RatioGauge.Ratio.of(getThreads() - getIdleThreads(), getMaxThreads()).getValue();
    }
}
