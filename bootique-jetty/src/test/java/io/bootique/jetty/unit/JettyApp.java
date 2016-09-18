package io.bootique.jetty.unit;

import com.google.inject.Module;
import io.bootique.jetty.JettyModule;
import io.bootique.test.BQDaemonTestRuntime;
import io.bootique.test.junit.BQDaemonTestFactory;
import org.eclipse.jetty.server.Server;

import java.util.function.Function;

public class JettyApp extends BQDaemonTestFactory {

    public void stop() {
        after();
    }

    public void startServer(String... args) {
        startServer(b -> {
        }, args);
    }

    public BQDaemonTestRuntime startServer(Module config, String... args) {
        Function<BQDaemonTestRuntime, Boolean> startupCheck = r -> r.getRuntime().getInstance(Server.class).isStarted();

        return app(args)
                .args("--server")
                .modules(JettyModule.class)
                .module(config)
                .startupCheck(startupCheck)
                .start();
    }
}
