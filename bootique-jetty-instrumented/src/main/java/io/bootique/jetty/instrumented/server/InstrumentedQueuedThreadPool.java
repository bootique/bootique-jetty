package io.bootique.jetty.instrumented.server;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.concurrent.BlockingQueue;

public class InstrumentedQueuedThreadPool extends QueuedThreadPool {

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

    @Override
    protected void doStart() throws Exception {

        super.doStart();

        metricRegistry.register(MetricRegistry.name(QueuedThreadPool.class, getName(), "utilization"),
                (Gauge<Double>) this::getUtilization);

        metricRegistry.register(MetricRegistry.name(QueuedThreadPool.class, getName(), "utilization-max"),
                (Gauge<Double>) this::getUtilizationMax);

        metricRegistry.register(MetricRegistry.name(QueuedThreadPool.class, getName(), "size"),
                (Gauge<Integer>) this::getThreads);

        metricRegistry.register(MetricRegistry.name(QueuedThreadPool.class, getName(), "queued-requests"),
                (Gauge<Integer>) this::getQueuedRequests);
    }

    protected int getQueuedRequests() {
        // This assumes the QueuedThreadPool is using a BlockingArrayQueue or ArrayBlockingQueue for its queue,
        // and is therefore a constant-time operation.
        return getQueue().size();
    }

    protected double getUtilization() {
        int threads = getThreads();
        return RatioGauge.Ratio.of(threads - getIdleThreads(), threads).getValue();
    }

    protected double getUtilizationMax() {

        // utilization-max is:
        //     (all_acceptor_t + all_selector_t + active_request_t) / maxT

        // This is not readily apparent from the Jetty API below. An explanation:
        //     getThreads()                    == all_acceptor_t + all_selector_t + active_request_t + idle_request_t
        // hence
        //     getThreads() - getIdleThreads() == all_acceptor_t + all_selector_t + active_request_t

        return RatioGauge.Ratio.of(getThreads() - getIdleThreads(), getMaxThreads()).getValue();
    }
}
