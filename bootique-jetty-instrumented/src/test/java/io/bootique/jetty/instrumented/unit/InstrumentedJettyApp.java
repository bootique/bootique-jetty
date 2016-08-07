package io.bootique.jetty.instrumented.unit;

import com.google.inject.Module;
import io.bootique.Bootique;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.instrumented.InstrumentedJettyModuleProvider;
import io.bootique.metrics.MetricsModule;
import io.bootique.test.BQDaemonTestRuntime;
import io.bootique.test.junit.BQDaemonTestFactory;
import org.eclipse.jetty.server.Server;

import java.util.function.Consumer;
import java.util.function.Function;

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
			bq.modules(JettyModule.class, MetricsModule.class)
					.module(new InstrumentedJettyModuleProvider()).module(config);
		};
		Function<BQDaemonTestRuntime, Boolean> startupCheck = r -> r.getRuntime().getInstance(Server.class).isStarted();

		return newRuntime().configurator(configurator).startupCheck(startupCheck).start(serverArgs);
	}

}
