/**
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * “License”); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jetty.instrumented.server;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.bootique.jetty.instrumented.JettyInstrumentedModule;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.concurrent.BlockingQueue;

public class InstrumentedQueuedThreadPool extends QueuedThreadPool {

    // for now assuming a single thread pool, so we can hardcode its name
    private static final String sizeMetric = JettyInstrumentedModule
            .METRIC_NAMING
            .name("ThreadPool", "Size");

    @Deprecated(forRemoval = true)
    private static final String queuedRequestsMetric = JettyInstrumentedModule
            .METRIC_NAMING
            .name("ThreadPool", "QueuedRequests");

    private static final String utilization = JettyInstrumentedModule
            .METRIC_NAMING
            .name("ThreadPool", "Utilization");

    private final MetricRegistry metricRegistry;

    public InstrumentedQueuedThreadPool(
            int maxThreads,
            int minThreads,
            int idleTimeout,
            MetricRegistry metricRegistry) {

        super(maxThreads, minThreads, idleTimeout);
        this.metricRegistry = metricRegistry;
    }

    /**
     * @deprecated unused
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public InstrumentedQueuedThreadPool(
            int maxThreads,
            int minThreads,
            int idleTimeout,
            BlockingQueue<Runnable> queue,
            MetricRegistry metricRegistry) {

        super(maxThreads, minThreads, idleTimeout, queue);
        this.metricRegistry = metricRegistry;
    }

    /**
     * @deprecated unused
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public InstrumentedQueuedThreadPool(MetricRegistry metricRegistry) {
        super();
        this.metricRegistry = metricRegistry;
    }

    /**
     * @deprecated unused
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
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

    /**
     * @deprecated unused
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public InstrumentedQueuedThreadPool(
            int maxThreads,
            int minThreads,
            MetricRegistry metricRegistry) {

        super(maxThreads, minThreads);
        this.metricRegistry = metricRegistry;
    }

    /**
     * @deprecated unused
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public InstrumentedQueuedThreadPool(
            int maxThreads,
            MetricRegistry metricRegistry) {

        super(maxThreads);
        this.metricRegistry = metricRegistry;
    }

    public static String sizeMetric() {
        return sizeMetric;
    }

    /**
     * @deprecated as we are no longer measuring the actual queued jobs
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
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

        // this metric is deprecated and will always return zero until removed in the future
        metricRegistry.register(queuedRequestsMetric(), (Gauge<Integer>) this::getQueueSizeZero);
        metricRegistry.register(utilizationMetric(), (Gauge<Double>) this::getUtilizationRate);
    }

    // Always return zero, as we are no longer measuring the actual queued jobs. "super.getQueueSize()" no longer
    // matches the number of queued requests, and is often a larger number
    @Deprecated(since = "4.0.0", forRemoval = true)
    private int getQueueSizeZero() {
        return 0;
    }
}
