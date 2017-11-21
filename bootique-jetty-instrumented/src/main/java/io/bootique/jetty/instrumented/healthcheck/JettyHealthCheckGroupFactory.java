package io.bootique.jetty.instrumented.healthcheck;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.metrics.health.HealthCheck;
import io.bootique.metrics.health.check.DoubleRange;
import io.bootique.metrics.health.check.IntRange;
import io.bootique.metrics.health.check.ValueRangeCheck;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 0.25
 */
@BQConfig("Configures Jetty-related health checks.")
public class JettyHealthCheckGroupFactory {

    static final String THREAD_POOL_UTILIZATION_CHECK = "bq.jetty.threadPool.utilization";
    static final String QUEUED_REQUESTS_CHECK = "bq.jetty.threadPool.queuedRequests";

    private IntRange queuedRequestsThreshold;
    private DoubleRange poolUtilizationThreshold;

    protected IntRange getQueuedRequestsThreshold() {
        return queuedRequestsThreshold != null ? queuedRequestsThreshold : new IntRange(3, 15);
    }

    @BQConfigProperty
    public void setQueuedRequestsThreshold(IntRange queuedRequestsThreshold) {
        this.queuedRequestsThreshold = queuedRequestsThreshold;
    }

    protected DoubleRange getPoolUtilizationThreshold() {
        return poolUtilizationThreshold != null ? poolUtilizationThreshold : new DoubleRange(0.7, 0.9);
    }

    @BQConfigProperty
    public void setPoolUtilizationThreshold(DoubleRange poolUtilizationThreshold) {
        this.poolUtilizationThreshold = poolUtilizationThreshold;
    }

    public JettyHealthCheckGroup createHealthCheckGroup(MetricRegistry registry) {
        return new JettyHealthCheckGroup(createHealthChecksMap(registry));
    }

    protected Map<String, HealthCheck> createHealthChecksMap(MetricRegistry registry) {
        Map<String, HealthCheck> checks = new HashMap<>(3);
        checks.put(THREAD_POOL_UTILIZATION_CHECK, createThreadPoolUtilizationCheck(registry));
        checks.put(QUEUED_REQUESTS_CHECK, createQueuedRequestsCheck(registry));
        return checks;
    }

    private HealthCheck createThreadPoolUtilizationCheck(MetricRegistry registry) {

        Gauge<Double> gauge = findGauge(Double.class, "utilization", registry);
        DoubleRange range = getPoolUtilizationThreshold();
        return new ValueRangeCheck<>(range, gauge::getValue);
    }

    private HealthCheck createQueuedRequestsCheck(MetricRegistry registry) {
        Gauge<Integer> gauge = findGauge(Integer.class, "queued-requests", registry);
        IntRange range = getQueuedRequestsThreshold();
        return new ValueRangeCheck<>(range, gauge::getValue);
    }

    private <T> Gauge<T> findGauge(Class<T> type, String label, MetricRegistry registry) {

        String name = MetricRegistry.name(QueuedThreadPool.class, "bootique-http", label);

        Collection<Gauge> gauges = registry.getGauges((n, m) -> name.equals(n)).values();
        switch (gauges.size()) {
            case 0:
                throw new IllegalArgumentException("Gauge not found: " + name);
            case 1:
                return gauges.iterator().next();
            default:
                throw new IllegalArgumentException("More than one Gauge matching the name: " + name);
        }
    }
}
