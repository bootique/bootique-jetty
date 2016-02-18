package com.nhl.bootique.jetty;

import org.eclipse.jetty.server.Server;
import org.junit.Before;

import com.google.inject.Guice;
import com.google.inject.Module;
import com.nhl.bootique.BQCoreModule;
import com.nhl.bootique.log.DefaultBootLogger;

public abstract class BaseITCase {

	private Module baseModule;
	private Module jettyModule;

	@Before
	public void before() {
		this.baseModule = BQCoreModule.builder().args(new String[] { "a1", "a2" })
				.bootLogger(new DefaultBootLogger(false)).build();
		this.jettyModule = new JettyModule();
	}

	protected Server createServer(Module extModule) {
		return Guice.createInjector(baseModule, jettyModule, extModule).getInstance(Server.class);
	}
}
