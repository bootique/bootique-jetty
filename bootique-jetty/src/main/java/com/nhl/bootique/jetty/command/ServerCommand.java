package com.nhl.bootique.jetty.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.nhl.bootique.cli.Cli;
import com.nhl.bootique.command.CommandMetadata;
import com.nhl.bootique.command.CommandOutcome;
import com.nhl.bootique.command.CommandWithMetadata;
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
                server.stop();
            } catch (Exception se) {
                return CommandOutcome.failed(1, se);
            }
        }

        return CommandOutcome.succeeded();
    }

}
