package com.nhl.bootique.jetty.test.junit;

import java.util.function.Function;

import org.eclipse.jetty.server.Server;
import org.junit.ClassRule;
import org.junit.Rule;

import com.nhl.bootique.BQRuntime;
import com.nhl.bootique.jetty.JettyModule;
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
		return super.newRuntime().startupCheck(startupCheck)
				.configurator(bootique -> bootique.module(JettyModule.class));
	}
}
