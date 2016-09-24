package io.bootique.jetty.unit;

import com.google.inject.Module;
import io.bootique.BQCoreModule;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.command.ServerCommand;
import io.bootique.test.BQDaemonTestRuntime;
import io.bootique.test.junit.BQDaemonTestFactory;
import org.eclipse.jetty.server.Server;

import java.util.function.Function;

public class JettyApp extends BQDaemonTestFactory {

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
                .modules(JettyModule.class)
                .module(config)
                .module(binder -> BQCoreModule.setDefaultCommand(binder, ServerCommand.class))
                .startupCheck(startupCheck)
                .start();
    }
}
