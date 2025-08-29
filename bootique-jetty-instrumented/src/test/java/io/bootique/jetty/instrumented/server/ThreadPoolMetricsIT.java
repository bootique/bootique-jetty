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
import io.bootique.BQRuntime;
import io.bootique.jetty.instrumented.unit.AssertExtras;
import io.bootique.jetty.instrumented.unit.ThreadPoolTester;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class ThreadPoolMetricsIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void utilization_1() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(1)
                .unblockAfterInProgressRequests(1)
                .afterStartup(r -> checkUtilization(r, 0))
                .afterRequestsFrozen(r -> checkUtilization(r, 1))
                .run("classpath:threads20.yml");
    }

    @Test
    public void utilization_3() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(3)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> checkUtilization(r, 0))
                .afterRequestsFrozen(r -> checkUtilization(r, 3))
                .run("classpath:threads20.yml");
    }


    @Deprecated(forRemoval = true)
    @Test
    public void queuedRequests_4() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(7)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> checkQueued(r, 0))

                // the check is deprecated, thresholds are ignored, and 0 is returned regardless of state
                .afterRequestsFrozen(r -> checkQueued(r, 0))
                .run("classpath:threads8.yml");
    }

    @Deprecated(forRemoval = true)
    private void checkQueued(BQRuntime runtime, int frozenRequests) {
        Gauge<Integer> gauge = findQueuedRequestsGauge(runtime);
        AssertExtras.assertWithRetry(() -> assertEquals(Integer.valueOf(frozenRequests), gauge.getValue()));
    }

    private void checkUtilization(BQRuntime runtime, int frozenRequests) {
        Gauge<Double> gauge = findUtilizationGauge(runtime);

        // usable = total - acceptors - selectors
        double usableThreads = 20 - 2 - 3;

        AssertExtras.assertWithRetry(() -> {
            double v = gauge.getValue();
            assertEquals(frozenRequests / usableThreads, v, 0.1,
                    "Actual frozen: " + (int) (v * usableThreads) + " vs expected " + frozenRequests);
        });
    }

    private Gauge<Double> findUtilizationGauge(BQRuntime runtime) {
        return findGauge(runtime, InstrumentedQueuedThreadPool.utilizationMetric());
    }

    @Deprecated(forRemoval = true)
    private Gauge<Integer> findQueuedRequestsGauge(BQRuntime runtime) {
        return findGauge(runtime, InstrumentedQueuedThreadPool.queuedRequestsMetric());
    }

    private <T> Gauge<T> findGauge(BQRuntime runtime, String metricName) {

        MetricRegistry registry = runtime.getInstance(MetricRegistry.class);
        Collection<Gauge> gauges = registry.getGauges((n, m) -> metricName.equals(n)).values();
        assertEquals(1, gauges.size(), "Unexpected number of gauges for " + metricName);
        return gauges.iterator().next();
    }
}
