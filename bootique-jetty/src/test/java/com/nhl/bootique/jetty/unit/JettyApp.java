package com.nhl.bootique.jetty.unit;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jetty.server.Server;

import com.google.inject.Module;
import com.nhl.bootique.BQRuntime;
import com.nhl.bootique.Bootique;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.test.junit.BQDaemonTestFactory;

public class JettyApp extends BQDaemonTestFactory {

	public void stop() {
		after();
	}

	public void start(Module config) {
		startWithArgs(config, "--server");
	}

	public void startWithArgs(Module config, String... args) {

		Consumer<Bootique> configurator = bq -> {
			bq.modules(JettyModule.class).module(config);
		};
		Function<BQRuntime, Boolean> startupCheck = r -> r.getInstance(Server.class).isStarted();

		newRuntime().configurator(configurator).startupCheck(startupCheck).start(args);
	}
}
