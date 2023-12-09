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

import com.codahale.metrics.MetricRegistry;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.jetty.MappedFilter;
import io.bootique.jetty.MappedListener;
import io.bootique.jetty.MappedServlet;
import io.bootique.jetty.instrumented.healthcheck.JettyHealthChecks;
import io.bootique.jetty.instrumented.healthcheck.JettyHealthChecksFactory;
import io.bootique.jetty.request.RequestMDCManager;
import io.bootique.jetty.server.ServerFactory;
import io.bootique.jetty.server.ServletContextHandlerExtender;
import io.bootique.shutdown.ShutdownManager;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.inject.Inject;
import java.util.EventListener;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

@BQConfig
public class InstrumentedServerFactory extends ServerFactory {

    private final MetricRegistry metricRegistry;

    private JettyHealthChecksFactory health;

    @Inject
    public InstrumentedServerFactory(
            Set<Servlet> diServlets,
            Set<MappedServlet> mappedServlets,
            Set<Filter> diFilters,
            Set<MappedFilter> mappedFilters,
            Set<EventListener> listeners,
            Set<MappedListener> mappedListeners,
            Set<ServletContextHandlerExtender> contextHandlerExtenders,
            RequestMDCManager mdcManager,
            ShutdownManager shutdownManager,
            MetricRegistry metricRegistry) {

        super(
                diServlets,
                mappedServlets,
                diFilters,
                mappedFilters,
                listeners,
                mappedListeners,
                contextHandlerExtenders,
                mdcManager,
                shutdownManager);

        this.metricRegistry = metricRegistry;
    }

    @BQConfigProperty("Configures Jetty healthcheck thresholds")
    public void setHealth(JettyHealthChecksFactory health) {
        this.health = health;
    }

    @Override
    protected QueuedThreadPool createThreadPool(BlockingQueue<Runnable> queue) {
        return new InstrumentedQueuedThreadPool(
                maxThreads,
                minThreads,
                idleThreadTimeout,
                metricRegistry);
    }

    public JettyHealthChecks createHealthChecks(MetricRegistry metricRegistry) {
        return getHealth().createHealthCheckGroup(metricRegistry);
    }

    JettyHealthChecksFactory getHealth() {
        return health != null ? health : new JettyHealthChecksFactory();
    }
}
