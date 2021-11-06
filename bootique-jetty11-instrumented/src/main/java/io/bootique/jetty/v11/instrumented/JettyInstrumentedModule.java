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

package io.bootique.jetty.v11.instrumented;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.di.TypeLiteral;
import io.bootique.jetty.v11.instrumented.healthcheck.JettyHealthChecks;
import io.bootique.jetty.v11.instrumented.request.RequestTimer;
import io.bootique.jetty.v11.instrumented.request.TransactionMDCItem;
import io.bootique.jetty.v11.instrumented.server.InstrumentedServerFactory;
import io.bootique.jetty.v11.JettyModule;
import io.bootique.jetty.v11.MappedListener;
import io.bootique.jetty.v11.server.ServerFactory;
import io.bootique.metrics.MetricNaming;
import io.bootique.metrics.health.HealthCheckModule;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;

import javax.inject.Singleton;

public class JettyInstrumentedModule extends ConfigModule {

    public static final MetricNaming METRIC_NAMING = MetricNaming.forModule(JettyInstrumentedModule.class);

    public static final int REQUEST_TIMER_LISTENER_ORDER = Integer.MIN_VALUE + 1000;

    @Override
    protected String defaultConfigPrefix() {
        // reusing overridden module prefix
        return "jetty";
    }

    @Override
    public void configure(Binder binder) {
        JettyModule.extend(binder)
                .addMappedListener(new TypeLiteral<MappedListener<RequestTimer>>() {
                })
                .addRequestMDCItem(TransactionIdMDC.MDC_KEY, TransactionMDCItem.class);

        HealthCheckModule.extend(binder).addHealthCheckGroup(JettyHealthChecks.class);
    }

    @Provides
    ServerFactory providerServerFactory(InstrumentedServerFactory serverFactory) {
        return serverFactory;
    }

    @Provides
    InstrumentedServerFactory providerInstrumentedServerFactory(ConfigurationFactory configFactory, MetricRegistry metricRegistry) {
        return config(InstrumentedServerFactory.class, configFactory).initMetricRegistry(metricRegistry);
    }

    @Provides
    @Singleton
    MappedListener<RequestTimer> provideRequestTimer(MetricRegistry metricRegistry) {
        String name = MetricNaming.forModule(JettyInstrumentedModule.class).name("Request", "Time");
        Timer timer = metricRegistry.timer(name);
        RequestTimer requestTimer = new RequestTimer(timer);
        return new MappedListener<>(requestTimer, REQUEST_TIMER_LISTENER_ORDER);
    }

    @Provides
    @Singleton
    TransactionMDCItem provideTransactionMDCItem(TransactionIdGenerator generator, TransactionIdMDC mdc) {
        return new TransactionMDCItem(generator, mdc);
    }

    @Singleton
    @Provides
    JettyHealthChecks provideHealthCheckGroup(InstrumentedServerFactory serverFactory) {
        return serverFactory.createHealthCheckGroup();
    }
}
