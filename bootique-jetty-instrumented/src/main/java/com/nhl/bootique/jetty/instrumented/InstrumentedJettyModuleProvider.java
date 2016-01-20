package com.nhl.bootique.jetty.instrumented;

import com.google.inject.Module;
import com.nhl.bootique.BQModuleProvider;

/**
 * @since 0.11
 */
public class InstrumentedJettyModuleProvider implements BQModuleProvider {

	@Override
	public Module module() {
		return new InstrumentedJettyModule();
	}
}
