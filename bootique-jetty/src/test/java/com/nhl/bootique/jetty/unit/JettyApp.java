package com.nhl.bootique.jetty.unit;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jetty.server.Server;

import com.google.inject.Module;
import com.nhl.bootique.Bootique;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.test.BQDaemonTestRuntime;
import com.nhl.bootique.test.junit.BQDaemonTestFactory;

public class JettyApp extends BQDaemonTestFactory {

	public void stop() {
		after();
	}

	public void startServer(String... args) {
		startServer(b -> {
		} , args);
	}

	public void startServer(Module config, String... args) {

		int len = args != null ? args.length + 1 : 1;

		String[] serverArgs = new String[len];
		serverArgs[0] = "--server";
		if (len > 1) {
			System.arraycopy(args, 0, serverArgs, 1, args.length);
		}

		Consumer<Bootique> configurator = bq -> {
			bq.modules(JettyModule.class).module(config);
		};
		Function<BQDaemonTestRuntime, Boolean> startupCheck = r -> r.getRuntime().getInstance(Server.class).isStarted();

		newRuntime().configurator(configurator).startupCheck(startupCheck).start(serverArgs);
	}
}
