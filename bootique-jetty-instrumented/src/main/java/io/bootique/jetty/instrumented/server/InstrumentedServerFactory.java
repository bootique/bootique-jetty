package io.bootique.jetty.instrumented.server;

import com.codahale.metrics.MetricRegistry;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.jetty.instrumented.healthcheck.JettyHealthCheckGroup;
import io.bootique.jetty.instrumented.healthcheck.JettyHealthCheckGroupFactory;
import io.bootique.jetty.server.ServerFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/**
 * @since 0.11
 */
@BQConfig
public class InstrumentedServerFactory extends ServerFactory {

    private JettyHealthCheckGroupFactory health;

    private MetricRegistry metricRegistry;

    public InstrumentedServerFactory initMetricRegistry(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        return this;
    }

    @Override
    protected QueuedThreadPool createThreadPool(BlockingQueue<Runnable> queue) {
        return new InstrumentedQueuedThreadPool(maxThreads, minThreads, idleThreadTimeout, getMetricRegistry());
    }

    /**
     * @return a new configured {@link JettyHealthCheckGroup}.
     * @since 0.25
     */
    public JettyHealthCheckGroup createHealthCheckGroup() {
        return getHealth().createHealthCheckGroup(getMetricRegistry());
    }

    JettyHealthCheckGroupFactory getHealth() {
        return health != null ? health : new JettyHealthCheckGroupFactory();
    }

    /**
     * @param health a factory for Jetty-related health checks.
     * @since 0.25
     */
    @BQConfigProperty
    public void setHealth(JettyHealthCheckGroupFactory health) {
        this.health = health;
    }

    protected MetricRegistry getMetricRegistry() {
        return Objects.requireNonNull(metricRegistry,
                "Factory is in invalid state. 'metricRegistry' was not initialized");
    }
}
