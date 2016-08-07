package io.bootique.jetty;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;

public class JettyModuleProvider implements BQModuleProvider {

	@Override
	public Module module() {
		return new JettyModule();
	}
}
