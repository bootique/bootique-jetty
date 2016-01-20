package com.nhl.bootique.jetty.command;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.nhl.bootique.command.CommandOutcome;
import com.nhl.bootique.command.OptionTriggeredCommand;
import com.nhl.bootique.jopt.Options;

import joptsimple.OptionParser;

public class ServerCommand extends OptionTriggeredCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerCommand.class);
	private static final String SERVER_OPTION = "server";

	private Provider<Server> serverProvider;

	@Inject
	public ServerCommand(Provider<Server> serverProvider) {
		this.serverProvider = serverProvider;
	}

	@Override
	protected CommandOutcome doRun(Options options) {
		LOGGER.info("Starting jetty...");

		Server server = serverProvider.get();
		try {
			server.start();
		} catch (Exception e) {
			return CommandOutcome.failed(1, e);
		}

		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			return CommandOutcome.failed(1, e);
		}

		return CommandOutcome.succeeded();
	}

	@Override
	protected String getOption() {
		return SERVER_OPTION;
	}

	@Override
	public void configOptions(OptionParser parser) {
		parser.accepts(getOption(), "Starts Jetty server");
	}
}
