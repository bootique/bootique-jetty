package com.nhl.bootique.jetty.unit;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jetty.server.Server;
import org.junit.rules.ExternalResource;

import com.google.inject.Module;
import com.nhl.bootique.BQRuntime;
import com.nhl.bootique.Bootique;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.test.BQDaemonTestRuntime;

public class JettyApp extends ExternalResource {

	private BQDaemonTestRuntime app;

	@Override
	protected void after() {
		if (app != null) {
			BQDaemonTestRuntime localApp = app;
			app = null;
			localApp.stop();
		}
	}

	public void stop() {
		after();
	}

	public void start(Module config) {

		Consumer<Bootique> configurator = bq -> {
			bq.modules(JettyModule.class).module(config);
		};
		Function<BQRuntime, Boolean> startupCheck = r -> r.getInstance(Server.class).isStarted();

		this.app = new BQDaemonTestRuntime(configurator, startupCheck);
		this.app.start(5, TimeUnit.SECONDS, "--server");
	}
}
