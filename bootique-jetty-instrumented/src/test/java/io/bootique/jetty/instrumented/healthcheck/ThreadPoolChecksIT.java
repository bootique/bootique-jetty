package io.bootique.jetty.instrumented.healthcheck;

import io.bootique.BQRuntime;
import io.bootique.jetty.instrumented.unit.AssertExtras;
import io.bootique.jetty.instrumented.unit.ThreadPoolTester;
import io.bootique.metrics.health.HealthCheckOutcome;
import io.bootique.metrics.health.HealthCheckRegistry;
import io.bootique.metrics.health.HealthCheckStatus;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ThreadPoolChecksIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testUtilization_2() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(2)
                .unblockAfterInProgressRequests(2)
                .afterStartup(r -> testUtilizationCheck(r, HealthCheckStatus.OK))
                .afterRequestsFrozen(r -> testUtilizationCheck(r, HealthCheckStatus.WARNING))
                .run("classpath:health8.yml");
    }

    @Test
    public void testUtilization_3() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(3)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> testUtilizationCheck(r, HealthCheckStatus.OK))
                .afterRequestsFrozen(r -> testUtilizationCheck(r, HealthCheckStatus.CRITICAL))
                .run("classpath:health8.yml");
    }

    @Test
    public void testQueuedRequests_5() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(5)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> testQueuedCheck(r, HealthCheckStatus.OK))
                .afterRequestsFrozen(r -> testQueuedCheck(r, HealthCheckStatus.WARNING))
                .run("classpath:health8.yml");
    }

    @Test
    public void testQueuedRequests_6() throws InterruptedException {

        new ThreadPoolTester(testFactory)
                .sendRequests(6)
                .unblockAfterInProgressRequests(3)
                .afterStartup(r -> testQueuedCheck(r, HealthCheckStatus.OK))
                .afterRequestsFrozen(r -> testQueuedCheck(r, HealthCheckStatus.CRITICAL))
                .run("classpath:health8.yml");
    }

    private void testUtilizationCheck(BQRuntime runtime, HealthCheckStatus expectedStatus) {
        HealthCheckRegistry registry = runtime.getInstance(HealthCheckRegistry.class);

        AssertExtras.assertWithRetry(() -> {
            HealthCheckOutcome result = registry.runHealthCheck(JettyHealthChecksFactory.POOL_UTILIZATION_CHECK);
            assertEquals(result.toString(), expectedStatus, result.getStatus());
        });
    }

    private void testQueuedCheck(BQRuntime runtime, HealthCheckStatus expectedStatus) {
        HealthCheckRegistry registry = runtime.getInstance(HealthCheckRegistry.class);

        AssertExtras.assertWithRetry(() -> {
            HealthCheckOutcome result = registry.runHealthCheck(JettyHealthChecksFactory.QUEUED_REQUESTS_CHECK);
            assertEquals(result.toString(), expectedStatus, result.getStatus());
        });
    }
}
