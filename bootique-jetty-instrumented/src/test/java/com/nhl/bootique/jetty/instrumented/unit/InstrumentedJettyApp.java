package com.nhl.bootique.jetty.instrumented.unit;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jetty.server.Server;

import com.google.inject.Module;
import com.nhl.bootique.Bootique;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.jetty.instrumented.InstrumentedJettyModuleProvider;
import com.nhl.bootique.logback.LogbackModule;
import com.nhl.bootique.metrics.MetricsModule;
import com.nhl.bootique.test.BQDaemonTestRuntime;
import com.nhl.bootique.test.junit.BQDaemonTestFactory;

public class InstrumentedJettyApp extends BQDaemonTestFactory {

	public void stop() {
		after();
	}

	public void startServer(String... args) {
		startServer(b -> {
		}, args);
	}

	public BQDaemonTestRuntime startServer(Module config, String... args) {

		int len = args != null ? args.length + 1 : 1;

		String[] serverArgs = new String[len];
		serverArgs[0] = "--server";
		if (len > 1) {
			System.arraycopy(args, 0, serverArgs, 1, args.length);
		}

		Consumer<Bootique> configurator = bq -> {
			bq.modules(JettyModule.class, MetricsModule.class, LogbackModule.class)
					.module(new InstrumentedJettyModuleProvider()).module(config);
		};
		Function<BQDaemonTestRuntime, Boolean> startupCheck = r -> r.getRuntime().getInstance(Server.class).isStarted();

		return newRuntime().configurator(configurator).startupCheck(startupCheck).start(serverArgs);
	}

}
