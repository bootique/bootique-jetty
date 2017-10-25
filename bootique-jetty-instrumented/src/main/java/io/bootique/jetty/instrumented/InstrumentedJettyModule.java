package io.bootique.jetty.instrumented;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.instrumented.request.RequestTimer;
import io.bootique.jetty.instrumented.server.InstrumentedServerFactory;
import io.bootique.jetty.server.ServerFactory;

/**
 * @since 0.11
 */
public class InstrumentedJettyModule extends ConfigModule {

    public InstrumentedJettyModule() {
        // reusing overridden module prefix
        super("jetty");
    }

    public InstrumentedJettyModule(String configPrefix) {
        super(configPrefix);
    }

    @Override
    public void configure(Binder binder) {
        JettyModule.extend(binder).addListener(RequestTimer.class);
    }

    @Provides
    ServerFactory providerServerFactory(ConfigurationFactory configFactory, MetricRegistry metricRegistry) {
        return configFactory.config(InstrumentedServerFactory.class, configPrefix).initMetricRegistry(metricRegistry);
    }

    @Provides
    @Singleton
    RequestTimer provideRequestTimer(MetricRegistry metricRegistry) {
        return new RequestTimer(metricRegistry);
    }
}
