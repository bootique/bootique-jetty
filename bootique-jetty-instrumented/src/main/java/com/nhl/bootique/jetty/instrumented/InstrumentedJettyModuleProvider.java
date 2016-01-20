package com.nhl.bootique.jetty.instrumented;

import java.util.Collection;
import java.util.Collections;

import com.google.inject.Module;
import com.nhl.bootique.BQModuleProvider;
import com.nhl.bootique.jetty.JettyModule;

/**
 * @since 0.11
 */
public class InstrumentedJettyModuleProvider implements BQModuleProvider {

	@Override
	public Module module() {
		return new InstrumentedJettyModule();
	}

	@Override
	public Collection<Class<? extends Module>> overrides() {
		return Collections.singleton(JettyModule.class);
	}
}
