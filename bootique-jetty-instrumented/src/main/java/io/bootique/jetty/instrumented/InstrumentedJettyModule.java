package io.bootique.jetty.instrumented;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Provides;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
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

	@Provides
	public ServerFactory createServerFactory(ConfigurationFactory configFactory, MetricRegistry metricRegistry) {
		return configFactory.config(InstrumentedServerFactory.class, configPrefix).initMetricRegistry(metricRegistry);
	}

}
