package io.bootique.jetty.instrumented;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.jetty.JettyModule;

import java.util.Collection;
import java.util.Collections;

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
