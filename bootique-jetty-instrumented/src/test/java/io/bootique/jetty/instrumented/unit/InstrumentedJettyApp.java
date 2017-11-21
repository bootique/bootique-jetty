package io.bootique.jetty.instrumented.unit;

import com.google.inject.Module;
import io.bootique.BQRuntime;
import io.bootique.test.junit.BQDaemonTestFactory;
import org.eclipse.jetty.server.Server;

import java.util.function.Function;

public class InstrumentedJettyApp extends BQDaemonTestFactory {

	public void stop() {
		after();
	}

	public BQRuntime start(String... args) {
		return start(b -> {
		}, args);
	}

	public BQRuntime start(Module config, String... args) {

		Function<BQRuntime, Boolean> startupCheck = r -> r.getInstance(Server.class).isStarted();

		return app("-s")
				.args(args)
				.autoLoadModules()
				.module(config)
				.startupCheck(startupCheck).start();
	}
}
