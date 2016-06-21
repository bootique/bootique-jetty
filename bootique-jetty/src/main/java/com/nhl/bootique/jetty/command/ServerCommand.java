package com.nhl.bootique.jetty.command;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.nhl.bootique.cli.Cli;
import com.nhl.bootique.command.CommandMetadata;
import com.nhl.bootique.command.CommandOutcome;
import com.nhl.bootique.command.CommandWithMetadata;

public class ServerCommand extends CommandWithMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCommand.class);

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

        LOGGER.info("Starting jetty...");

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
