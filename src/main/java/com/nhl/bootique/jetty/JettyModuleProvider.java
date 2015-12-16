package com.nhl.bootique.jetty;

import com.google.inject.Module;
import com.nhl.bootique.BQModuleProvider;

public class JettyModuleProvider implements BQModuleProvider {

	@Override
	public Module module() {
		return new JettyModule();
	}
}
