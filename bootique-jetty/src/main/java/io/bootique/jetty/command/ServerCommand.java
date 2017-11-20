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
            server.start();
        } catch (Exception e) {
            return CommandOutcome.failed(1, e);
        }

        try {
            Thread.currentThread().join();
        } catch (InterruptedException ie) {

            // interruption of a running Jetty daemon is a normal event, so unless we get shutdown errors, return success
            try {
                // TODO: probably redundant. The server will be stopped again in the shutdown thread.
                // see https://github.com/bootique/bootique/issues/197 : we may want to implement this using
                // "start and get out" strategy...
                server.stop();
            } catch (Exception se) {
                return CommandOutcome.failed(1, se);
            }
        }

        return CommandOutcome.succeeded();
    }

}
