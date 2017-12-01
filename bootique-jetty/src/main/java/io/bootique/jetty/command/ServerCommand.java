package io.bootique.jetty.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.meta.application.CommandMetadata;
import org.eclipse.jetty.server.Server;

public class ServerCommand extends CommandWithMetadata {

    private Provider<Server> serverProvider;

    @Inject
    public ServerCommand(Provider<Server> serverProvider) {
        super(createMetadata());
        this.serverProvider = serverProvider;
    }

    private static CommandMetadata createMetadata() {
        return CommandMetadata.builder(ServerCommand.class).description("Starts Jetty server.").build();
    }

    @Override
    public CommandOutcome run(Cli cli) {

        Server server = serverProvider.get();
        try {
            // this blocks until a successful start or an error, then releases current thread, while Jetty
            // stays running on the background
            server.start();
        } catch (Exception e) {
            return CommandOutcome.failed(1, e);
        }

        return CommandOutcome.succeededAndForkedToBackground();
    }

}
