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

package io.bootique.jetty.jakarta.instrumented.server;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.bootique.BQRuntime;
import io.bootique.jetty.jakarta.instrumented.unit.AssertExtras;
import io.bootique.jetty.jakarta.instrumented.unit.ThreadPoolTester;
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
            assertEquals((acceptors + selectors + frozenRequests) / 20d, v, 0.1,
                    "Actual frozen: " + (int) (v * 20d - acceptors - selectors) + " vs expected " + frozenRequests);
        });
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
        assertEquals(1, gauges.size(), "Unexpected number of gauges for " + metricName);
        return gauges.iterator().next();
    }
}
