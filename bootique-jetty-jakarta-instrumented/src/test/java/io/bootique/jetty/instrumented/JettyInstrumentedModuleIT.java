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

package io.bootique.jetty.instrumented;

import com.codahale.metrics.MetricRegistry;
import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQRuntimeChecker;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.metrics.MetricsModule;
import io.bootique.metrics.health.HealthCheckModule;
import io.bootique.metrics.health.HealthCheckRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class JettyInstrumentedModuleIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @Deprecated
    @Test
    public void moduleDeclaresDependencies() {
        final BQRuntime bqRuntime = testFactory.app().moduleProvider(new JettyInstrumentedModule()).createRuntime();
        BQRuntimeChecker.testModulesLoaded(bqRuntime,
                JettyModule.class,
                HealthCheckModule.class,
                MetricsModule.class
        );
    }

    @Test
    public void metrics() {
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
    public void healthChecks() {
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
