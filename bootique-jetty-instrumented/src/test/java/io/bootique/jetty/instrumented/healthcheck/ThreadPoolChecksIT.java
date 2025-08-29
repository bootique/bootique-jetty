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

package io.bootique.jetty.instrumented.healthcheck;

import io.bootique.BQRuntime;
import io.bootique.jetty.instrumented.unit.AssertExtras;
import io.bootique.jetty.instrumented.unit.ThreadPoolTester;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.metrics.health.HealthCheckOutcome;
import io.bootique.metrics.health.HealthCheckRegistry;
import io.bootique.metrics.health.HealthCheckStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class ThreadPoolChecksIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void utilization_2() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(2)
                .unblockAfterInProgressRequests(2)
                .afterStartup(r -> testUtilizationCheck(r, HealthCheckStatus.OK))
                .afterRequestsFrozen(r -> testUtilizationCheck(r, HealthCheckStatus.WARNING))
                .run("classpath:health8.yml");
    }

    @Test
    public void utilization_3() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(3)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> testUtilizationCheck(r, HealthCheckStatus.OK))
                .afterRequestsFrozen(r -> testUtilizationCheck(r, HealthCheckStatus.CRITICAL))
                .run("classpath:health8.yml");
    }

    @Test
    @Deprecated(forRemoval = true)
    public void queuedRequests_5() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(5)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> testQueuedCheck(r, HealthCheckStatus.OK))

                // the check is deprecated, thresholds are ignored, and success is returned regardless of state
                .afterRequestsFrozen(r -> testQueuedCheck(r, HealthCheckStatus.OK))
                .run("classpath:health8.yml");
    }

    private void testUtilizationCheck(BQRuntime runtime, HealthCheckStatus expectedStatus) {
        HealthCheckRegistry registry = runtime.getInstance(HealthCheckRegistry.class);

        AssertExtras.assertWithRetry(() -> {
            HealthCheckOutcome result = registry.runHealthCheck(JettyHealthChecksFactory.POOL_UTILIZATION_CHECK);
            assertEquals(expectedStatus, result.getStatus(), result.toString());
        });
    }

    @Deprecated(forRemoval = true)
    private void testQueuedCheck(BQRuntime runtime, HealthCheckStatus expectedStatus) {
        HealthCheckRegistry registry = runtime.getInstance(HealthCheckRegistry.class);

        AssertExtras.assertWithRetry(() -> {
            HealthCheckOutcome result = registry.runHealthCheck(JettyHealthChecksFactory.QUEUED_REQUESTS_CHECK);
            assertEquals(expectedStatus, result.getStatus(), result.toString());
        });
    }
}
