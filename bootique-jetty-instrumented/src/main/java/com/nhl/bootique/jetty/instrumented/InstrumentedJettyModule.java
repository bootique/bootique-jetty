package com.nhl.bootique.jetty.instrumented;

import java.util.Collections;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.nhl.bootique.ConfigModule;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.jetty.MappedFilter;
import com.nhl.bootique.jetty.instrumented.request.TimingFilter;
import com.nhl.bootique.jetty.instrumented.server.InstrumentedServerFactory;
import com.nhl.bootique.jetty.server.ServerFactory;

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
		JettyModule.contributeMappedFilters(binder).addBinding()
				.to(Key.get(MappedFilter.class, InstrumentedRequestFilter.class));
	}

	@InstrumentedRequestFilter
	@Provides
	@Singleton
	private MappedFilter provideMappedRequestFilter(TimingFilter filter) {
		return new MappedFilter(filter, Collections.singleton("/*"), TimingFilter.DEFAULT_ORDER);
	}

	@Provides
	@Singleton
	private TimingFilter provideInstrumentedRequestFilter(MetricRegistry metricRegistry) {
		return new TimingFilter(metricRegistry);
	}

	@Provides
	public ServerFactory createServerFactory(ConfigurationFactory configFactory, MetricRegistry metricRegistry) {
		return configFactory.config(InstrumentedServerFactory.class, configPrefix).initMetricRegistry(metricRegistry);
	}

}
