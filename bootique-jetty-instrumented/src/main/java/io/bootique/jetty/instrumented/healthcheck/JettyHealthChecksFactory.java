/**
 *  Licensed to ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.jetty.instrumented.healthcheck;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.jetty.instrumented.JettyInstrumentedModule;
import io.bootique.jetty.instrumented.server.InstrumentedQueuedThreadPool;
import io.bootique.metrics.health.HealthCheck;
import io.bootique.metrics.health.check.IntRangeFactory;
import io.bootique.metrics.health.check.PercentRangeFactory;
import io.bootique.metrics.health.check.ValueRange;
import io.bootique.metrics.health.check.ValueRangeCheck;
import io.bootique.value.Percent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@BQConfig("Configures Jetty-related health checks.")
public class JettyHealthChecksFactory {

    static final String POOL_UTILIZATION_CHECK = JettyInstrumentedModule
            .METRIC_NAMING
            .name("ThreadPool", "Utilization");
    
    static final String QUEUED_REQUESTS_CHECK =  JettyInstrumentedModule
            .METRIC_NAMING
            .name("ThreadPool", "QueuedRequests");

    private PercentRangeFactory poolUtilizationThresholds;
    private IntRangeFactory queuedRequestsThresholds;

    protected ValueRange<Integer> getQueuedRequestsThresholds() {

        // init min if it wasn't set...
        if (queuedRequestsThresholds != null) {
            if (queuedRequestsThresholds.getMin() == null) {
                queuedRequestsThresholds.setMin(0);
            }

            return queuedRequestsThresholds.createRange();
        }

        // TODO: what is the default max queued requests for our Jetty?
        return ValueRange.builder(Integer.class).min(0).warning(3).critical(15).build();
    }

    @BQConfigProperty
    public void setQueuedRequestsThresholds(IntRangeFactory queuedRequestsThresholds) {
        this.queuedRequestsThresholds = queuedRequestsThresholds;
    }

    protected ValueRange<Percent> getPoolUtilizationThresholds() {

        if (poolUtilizationThresholds != null) {
            return poolUtilizationThresholds.createRange();
        }

        // default range
        return ValueRange.builder(Percent.class)
                .min(Percent.ZERO)
                .warning(new Percent(0.7))
                .critical(new Percent(0.9))
                .max(Percent.HUNDRED).build();
    }

    @BQConfigProperty
    public void setPoolUtilizationThresholds(PercentRangeFactory poolUtilizationThresholds) {
        this.poolUtilizationThresholds = poolUtilizationThresholds;
    }

    public JettyHealthChecks createHealthCheckGroup(MetricRegistry registry) {
        return new JettyHealthChecks(createHealthChecksMap(registry));
    }

    protected Map<String, HealthCheck> createHealthChecksMap(MetricRegistry registry) {
        Map<String, HealthCheck> checks = new HashMap<>(3);
        checks.put(POOL_UTILIZATION_CHECK, createThreadPoolUtilizationCheck(registry));
        checks.put(QUEUED_REQUESTS_CHECK, createQueuedRequestsCheck(registry));
        return checks;
    }

    private HealthCheck createThreadPoolUtilizationCheck(MetricRegistry registry) {
        Supplier<Double> deferredGauge = valueFromGauge(registry, InstrumentedQueuedThreadPool.utilizationMetric());
        Supplier<Percent> deferedPctGauge = () -> new Percent(deferredGauge.get());
        ValueRange<Percent> range = getPoolUtilizationThresholds();
        return new ValueRangeCheck<>(range, deferedPctGauge);
    }

    private HealthCheck createQueuedRequestsCheck(MetricRegistry registry) {
        Supplier<Integer> deferredGauge = valueFromGauge(registry, InstrumentedQueuedThreadPool.queuedRequestsMetric());
        ValueRange<Integer> range = getQueuedRequestsThresholds();
        return new ValueRangeCheck<>(range, deferredGauge);
    }

    private <T> Supplier<T> valueFromGauge(MetricRegistry registry, String name) {

        // using deferred gauge resolving to allow health checks against the system with misconfigured metrics,
        // or Jetty not yet up during health check creation
        return () -> (T) findGauge(registry, name).getValue();
    }

    private <T> Gauge<T> findGauge(MetricRegistry registry, String name) {

        Collection<Gauge> gauges = registry.getGauges((n, m) -> name.equals(n)).values();
        return switch (gauges.size()) {
            case 0 -> throw new IllegalArgumentException("Gauge not found: " + name);
            case 1 -> gauges.iterator().next();
            default -> throw new IllegalArgumentException("More than one Gauge matching the name: " + name);
        };
    }
}
