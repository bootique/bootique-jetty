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

package io.bootique.jetty.instrumented.server;

import com.codahale.metrics.MetricRegistry;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.di.Injector;
import io.bootique.jetty.instrumented.healthcheck.JettyHealthChecks;
import io.bootique.jetty.instrumented.healthcheck.JettyHealthChecksFactory;
import io.bootique.jetty.server.ServerFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.concurrent.BlockingQueue;

@BQConfig
public class InstrumentedServerFactory extends ServerFactory {

    private JettyHealthChecksFactory health;

    @BQConfigProperty("Configures Jetty healthcheck thresholds")
    public void setHealth(JettyHealthChecksFactory health) {
        this.health = health;
    }

    @Override
    protected QueuedThreadPool createThreadPool(BlockingQueue<Runnable> queue, Injector injector) {
        return new InstrumentedQueuedThreadPool(
                maxThreads,
                minThreads,
                idleThreadTimeout,
                injector.getInstance(MetricRegistry.class));
    }

    public JettyHealthChecks createHealthChecks(MetricRegistry metricRegistry) {
        return getHealth().createHealthCheckGroup(metricRegistry);
    }

    JettyHealthChecksFactory getHealth() {
        return health != null ? health : new JettyHealthChecksFactory();
    }
}
