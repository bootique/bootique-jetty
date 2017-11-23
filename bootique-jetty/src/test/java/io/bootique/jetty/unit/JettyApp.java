package io.bootique.jetty.unit;

import com.google.inject.Module;
import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.command.ServerCommand;
import io.bootique.test.junit.BQDaemonTestFactory;
import org.eclipse.jetty.server.Server;

import java.util.function.Function;

public class JettyApp extends BQDaemonTestFactory {

    public void stop() {
        after();
    }

    public BQRuntime start(String... args) {
        return start(b -> {
        }, args);
    }

    public BQRuntime start(Module config, String... args) {
        Function<BQRuntime, Boolean> startupCheck = r -> r.getInstance(Server.class).isStarted();

        return app(args)
                .modules(JettyModule.class)
                .module(config)
                .module(binder -> BQCoreModule.extend(binder).setDefaultCommand(ServerCommand.class))
                .startupCheck(startupCheck)
                .start();
    }
}
