package io.bootique.jetty.test.junit;

import io.bootique.jetty.JettyModule;
import io.bootique.test.BQDaemonTestRuntime;
import io.bootique.test.junit.BQDaemonTestFactory;
import org.eclipse.jetty.server.Server;
import org.junit.ClassRule;
import org.junit.Rule;

import java.util.Collection;
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
        Function<BQDaemonTestRuntime, Boolean> startupCheck = r -> r.getRuntime().getInstance(Server.class).isStarted();
        return new Builder(runtimes, args).startupCheck(startupCheck).modules(JettyModule.class);
    }

    /**
     * @return a new instance of builder for the test runtime stack.
     * @deprecated since 0.20 in favor of {@link #app(String...)}.
     */
    @Deprecated
    @Override
    public Builder newRuntime() {
        return app();
    }

    public static class Builder extends io.bootique.test.junit.BQDaemonTestFactory.Builder<Builder> {

        protected Builder(Collection<BQDaemonTestRuntime> runtimes, String[] args) {
            super(runtimes, args);
        }

        /**
         * @param args
         * @return a new {@link BQDaemonTestRuntime}.
         * @deprecated since 0.20 use no-arg {@link #startServer()}.
         */
        public BQDaemonTestRuntime startServer(String... args) {
            bootique.args(args);
            return startServer();
        }

        public BQDaemonTestRuntime startServer() {
            return args("--server").start();
        }
    }
}
