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

package io.bootique.jetty.instrumented;

import com.codahale.metrics.MetricRegistry;
import io.bootique.BQRuntime;
import io.bootique.metrics.health.HealthCheckRegistry;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class JettyInstrumentedModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testMetrics() {
        BQRuntime runtime = testFactory
                .app("--server")
                .autoLoadModules()
                .createRuntime();

        runtime.run();

        MetricRegistry metricRegistry = runtime.getInstance(MetricRegistry.class);

        Set<String> expectedTimers = new HashSet<>(asList("bq.Jetty.Request.Time"));
        assertEquals(expectedTimers, metricRegistry.getTimers().keySet());

        Set<String> expectedGauges = new HashSet<>(asList(
                "bq.Jetty.ThreadPool.Size",
                "bq.Jetty.ThreadPool.QueuedRequests",
                "bq.Jetty.ThreadPool.Utilization"));

        assertEquals(expectedGauges, metricRegistry.getGauges().keySet());
    }

    @Test
    public void testHealthChecks() {
        BQRuntime runtime = testFactory
                .app("--server")
                .autoLoadModules()
                .createRuntime();

        HealthCheckRegistry registry = runtime.getInstance(HealthCheckRegistry.class);

        Set<String> expectedGauges = new HashSet<>(asList(
                "bq.Jetty.ThreadPool.QueuedRequests",
                "bq.Jetty.ThreadPool.Utilization"));

        assertEquals(expectedGauges, registry.healthCheckNames());
    }
}
