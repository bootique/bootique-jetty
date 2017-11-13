package io.bootique.jetty.instrumented;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedListener;
import io.bootique.jetty.instrumented.request.RequestMDCManager;
import io.bootique.jetty.instrumented.request.RequestTimer;
import io.bootique.jetty.instrumented.server.InstrumentedServerFactory;
import io.bootique.jetty.server.ServerFactory;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;

/**
 * @since 0.11
 */
public class InstrumentedJettyModule extends ConfigModule {

    // the value should be low enough to ensure we wrap most normal listeners, yet allow insertion other wrappers
    // around the timer
    public static final int REQUEST_TIMER_LISTENER_ORDER = Integer.MIN_VALUE + 1000;

    // the value wrap around request timer
    public static final int BUSINESS_TX_LISTENER_ORDER = REQUEST_TIMER_LISTENER_ORDER - 1;

    public InstrumentedJettyModule() {
        // reusing overridden module prefix
        super("jetty");
    }

    public InstrumentedJettyModule(String configPrefix) {
        super(configPrefix);
    }

    @Override
    public void configure(Binder binder) {
        JettyModule.extend(binder)
                .addMappedListener(new TypeLiteral<MappedListener<RequestTimer>>() {
                })
                .addMappedListener(new TypeLiteral<MappedListener<RequestMDCManager>>() {
                });
    }

    @Provides
    ServerFactory providerServerFactory(ConfigurationFactory configFactory, MetricRegistry metricRegistry) {
        return configFactory.config(InstrumentedServerFactory.class, configPrefix).initMetricRegistry(metricRegistry);
    }

    @Provides
    @Singleton
    MappedListener<RequestTimer> provideRequestTimer(MetricRegistry metricRegistry) {
        RequestTimer timer = new RequestTimer(metricRegistry);
        return new MappedListener<>(timer, REQUEST_TIMER_LISTENER_ORDER);
    }

    @Provides
    @Singleton
    MappedListener<RequestMDCManager> provideRequestMDCManager(TransactionIdGenerator generator, TransactionIdMDC mdc) {
        RequestMDCManager mdcManager = new RequestMDCManager(generator, mdc);
        return new MappedListener<>(mdcManager, BUSINESS_TX_LISTENER_ORDER);
    }
}
