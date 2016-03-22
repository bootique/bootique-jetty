package com.nhl.bootique.jetty.test.junit;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jetty.server.Server;
import org.junit.ClassRule;
import org.junit.Rule;

import com.nhl.bootique.BQRuntime;
import com.nhl.bootique.Bootique;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.test.BQDaemonTestRuntime;
import com.nhl.bootique.test.junit.BQDaemonTestFactory;

/**
 * A unit test helper that starts a Bootique Jetty server.
 * 
 * <p>
 * Instances should be annotated within the unit tests with {@link Rule} or
 * {@link ClassRule}. E.g.:
 * 
 * <pre>
 * public class MyTest {
 * 
 * 	&#64;Rule
 * 	public JettyTestFactory testFactory = new JettyTestFactory();
 * }
 * </pre>
 * 
 * @since 0.13
 */
public class JettyTestFactory extends BQDaemonTestFactory {

	@Override
	public Builder newRuntime() {
		Function<BQRuntime, Boolean> startupCheck = r -> r.getInstance(Server.class).isStarted();
		return new Builder(runtimes).startupCheck(startupCheck)
				.configurator(bootique -> bootique.module(JettyModule.class));
	}

	public static class Builder extends com.nhl.bootique.test.junit.BQDaemonTestFactory.Builder {

		public Builder(Collection<BQDaemonTestRuntime> runtimes) {
			super(runtimes);
		}

		@Override
		public Builder startupCheck(Function<BQRuntime, Boolean> startupCheck) {
			super.startupCheck(startupCheck);
			return this;
		}

		@Override
		public Builder configurator(Consumer<Bootique> configurator) {
			super.configurator(configurator);
			return this;
		}

		@Override
		public Builder property(String key, String value) {
			super.property(key, value);
			return this;
		}

		@Override
		public Builder startupTimeout(long timeout, TimeUnit unit) {
			super.startupTimeout(timeout, unit);
			return this;
		}

		public void startServer(String... args) {

			int len = args != null ? args.length + 1 : 1;
			String[] newArgs = new String[len];
			newArgs[0] = "--server";

			if (len > 1) {
				System.arraycopy(args, 0, newArgs, 1, len - 1);
			}

			start(newArgs);
		}
	}
}
