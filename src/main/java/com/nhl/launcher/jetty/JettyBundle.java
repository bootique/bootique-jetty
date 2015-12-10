package com.nhl.launcher.jetty;

import org.eclipse.jetty.server.Server;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.nhl.launcher.BootstrapModule;
import com.nhl.launcher.command.Command;
import com.nhl.launcher.config.FactoryConfigurationService;
import com.nhl.launcher.env.DefaultEnvironment;
import com.nhl.launcher.jetty.command.ServerCommand;
import com.nhl.launcher.jetty.server.ServerFactory;

public class JettyBundle {

	private static final String CONFIG_PREFIX = "jetty";

	private String configPrefix;
	private String context;

	public static JettyBundle create() {
		return create(CONFIG_PREFIX);
	}

	public static JettyBundle create(String configPrefix) {
		return new JettyBundle(configPrefix);
	}

	private JettyBundle(String configPrefix) {
		this.configPrefix = configPrefix;
	}

	public JettyBundle context(String context) {
		this.context = context;
		return this;
	}

	public Module module() {
		return new JettyModule();
	}

	class JettyModule implements Module {

		@Override
		public void configure(Binder binder) {
			Multibinder.newSetBinder(binder, Command.class).addBinding().to(ServerCommand.class);

			if (context != null) {
				BootstrapModule.propertiesBinder(binder)
						.addBinding(DefaultEnvironment.FRAMEWORK_PROPERTIES_PREFIX + "." + configPrefix + ".context")
						.toInstance(context);
			}
		}

		@Provides
		public Server createServer(FactoryConfigurationService factoryService) {
			return factoryService.factory(ServerFactory.class, configPrefix).createServer();
		}
	}
}
