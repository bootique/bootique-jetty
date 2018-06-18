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
import com.codahale.metrics.Timer;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedListener;
import io.bootique.jetty.instrumented.healthcheck.JettyHealthChecks;
import io.bootique.jetty.instrumented.request.RequestMDCManager;
import io.bootique.jetty.instrumented.request.RequestTimer;
import io.bootique.jetty.instrumented.server.InstrumentedServerFactory;
import io.bootique.jetty.server.ServerFactory;
import io.bootique.metrics.MetricNaming;
import io.bootique.metrics.health.HealthCheckModule;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;

/**
 * @since 0.11
 */
public class JettyInstrumentedModule extends ConfigModule {

    public static final MetricNaming METRIC_NAMING = MetricNaming.forModule(JettyInstrumentedModule.class);

    // TX ID listener is usually the outermost listener in any app. It is a good idea to order your other listeners
    // relative to this one , using higher ordering values.
    public static final int BUSINESS_TX_LISTENER_ORDER = Integer.MIN_VALUE + 800;

    // goes inside BUSINESS_TX_LISTENER
    public static final int REQUEST_TIMER_LISTENER_ORDER = BUSINESS_TX_LISTENER_ORDER + 200;

    public JettyInstrumentedModule() {
        // reusing overridden module prefix
        super("jetty");
    }

    public JettyInstrumentedModule(String configPrefix) {
        super(configPrefix);
    }

    @Override
    public void configure(Binder binder) {
        JettyModule.extend(binder)
                .addMappedListener(new TypeLiteral<MappedListener<RequestTimer>>() {
                })
                .addMappedListener(new TypeLiteral<MappedListener<RequestMDCManager>>() {
                });

        HealthCheckModule.extend(binder).addHealthCheckGroup(JettyHealthChecks.class);
    }

    @Provides
    ServerFactory providerServerFactory(InstrumentedServerFactory serverFactory) {
        return serverFactory;
    }

    @Provides
    InstrumentedServerFactory providerInstrumentedServerFactory(ConfigurationFactory configFactory, MetricRegistry metricRegistry) {
        return configFactory.config(InstrumentedServerFactory.class, configPrefix).initMetricRegistry(metricRegistry);
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
    MappedListener<RequestMDCManager> provideRequestMDCManager(TransactionIdGenerator generator, TransactionIdMDC mdc) {
        RequestMDCManager mdcManager = new RequestMDCManager(generator, mdc);
        return new MappedListener<>(mdcManager, BUSINESS_TX_LISTENER_ORDER);
    }

    @Singleton
    @Provides
    JettyHealthChecks provideHealthCheckGroup(InstrumentedServerFactory serverFactory) {
        return serverFactory.createHealthCheckGroup();
    }
}
