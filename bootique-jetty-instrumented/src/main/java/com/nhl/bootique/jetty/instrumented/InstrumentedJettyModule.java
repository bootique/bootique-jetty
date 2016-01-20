package com.nhl.bootique.jetty.instrumented;

import com.nhl.bootique.ConfigModule;

public class InstrumentedJettyModule extends ConfigModule {

	public InstrumentedJettyModule() {
		// taking over overridden module prefix
		super("jetty");
	}

	public InstrumentedJettyModule(String configPrefix) {
		super(configPrefix);
	}
	
	

}
