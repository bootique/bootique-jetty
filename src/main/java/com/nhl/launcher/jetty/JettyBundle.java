package com.nhl.launcher.jetty;

import org.eclipse.jetty.server.Server;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.nhl.launcher.command.Command;
import com.nhl.launcher.config.ConfigurationFactory;
import com.nhl.launcher.jetty.command.ServerCommand;

public class JettyBundle {

	private static final String CONFIG_PREFIX = "jetty";

	private String configPrefix;

	public static JettyBundle create() {
		return create(CONFIG_PREFIX);
	}

	public static JettyBundle create(String configPrefix) {
		return new JettyBundle(configPrefix);
	}

	private JettyBundle(String configPrefix) {
		this.configPrefix = configPrefix;
	}

	public Module module() {
		return new JettyModule();
	}

	class JettyModule implements Module {

		@Override
		public void configure(Binder binder) {
			Multibinder.newSetBinder(binder, Command.class).addBinding().to(ServerCommand.class);
		}

		@Provides
		public Server createServer(ConfigurationFactory configFactory) {
			return configFactory.subconfig(configPrefix, JettyConfig.class).createServer();
		}

	}
}
