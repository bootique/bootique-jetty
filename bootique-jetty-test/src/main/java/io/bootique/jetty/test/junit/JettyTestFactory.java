package io.bootique.jetty.test.junit;

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.command.ServerCommand;
import io.bootique.test.junit.BQDaemonTestFactory;
import io.bootique.test.junit.BQRuntimeDaemon;
import org.eclipse.jetty.server.Server;
import org.junit.ClassRule;
import org.junit.Rule;

import java.util.Map;
import java.util.function.Function;

/**
 * A unit test helper that starts a Bootique Jetty server.
 * <p>
 * <p>
 * Instances should be annotated within the unit tests with {@link Rule} or
 * {@link ClassRule}. E.g.:
 * <p>
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

    /**
     * @return a new instance of builder for the test runtime stack.
     * @since 0.20
     */
    @Override
    public Builder app(String... args) {
        Function<BQRuntime, Boolean> startupCheck = r -> r.getInstance(Server.class).isStarted();
        return new Builder(runtimes, args).startupCheck(startupCheck).modules(JettyModule.class);
    }

    public static class Builder extends io.bootique.test.junit.BQDaemonTestFactory.Builder<Builder> {

        protected Builder(Map<BQRuntime, BQRuntimeDaemon> runtimes, String[] args) {
            super(runtimes, args);
        }

        @Override
        public BQRuntime createRuntime() {
            module(binder -> BQCoreModule.extend(binder).setDefaultCommand(ServerCommand.class));
            return super.createRuntime();
        }
    }
}
