package io.bootique.jetty.instrumented.unit;

import com.google.inject.Module;
import io.bootique.BQCoreModule;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.command.ServerCommand;
import io.bootique.jetty.instrumented.InstrumentedJettyModuleProvider;
import io.bootique.metrics.MetricsModule;
import io.bootique.test.BQDaemonTestRuntime;
import io.bootique.test.junit.BQDaemonTestFactory;
import org.eclipse.jetty.server.Server;

import java.util.function.Function;

public class InstrumentedJettyApp extends BQDaemonTestFactory {

	public void stop() {
		after();
	}

	public void start(String... args) {
		start(b -> {
		}, args);
	}

	public BQDaemonTestRuntime start(Module config, String... args) {

		Function<BQDaemonTestRuntime, Boolean> startupCheck = r -> r.getRuntime().getInstance(Server.class).isStarted();

		return app(args)
				.modules(JettyModule.class, MetricsModule.class)
				.module(new InstrumentedJettyModuleProvider())
				.module(config)
				.module(binder -> BQCoreModule.extend(binder).setDefaultCommand(ServerCommand.class))
				.startupCheck(startupCheck).start();
	}

}
