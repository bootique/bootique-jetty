package com.nhl.bootique.jetty;

import java.util.Map;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Server;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.nhl.bootique.BQModule;
import com.nhl.bootique.command.Command;
import com.nhl.bootique.env.DefaultEnvironment;
import com.nhl.bootique.factory.FactoryConfigurationService;
import com.nhl.bootique.jetty.command.ServerCommand;
import com.nhl.bootique.jetty.server.ServerFactory;

public class JettyBundle {

	private static final String CONFIG_PREFIX = "jetty";

	/**
	 * Utility method for other modules to contribute servlets to Jetty.
	 */
	public static MapBinder<String, Servlet> servletBinder(Binder binder) {
		return MapBinder.newMapBinder(binder, String.class, Servlet.class);
	}

	private String configPrefix;
	private String context;
	private int port;

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

	public JettyBundle port(int port) {
		this.port = port;
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
				BQModule.propertiesBinder(binder)
						.addBinding(DefaultEnvironment.FRAMEWORK_PROPERTIES_PREFIX + "." + configPrefix + ".context")
						.toInstance(context);
			}

			if (port > 0) {
				BQModule.propertiesBinder(binder)
						.addBinding(
								DefaultEnvironment.FRAMEWORK_PROPERTIES_PREFIX + "." + configPrefix + ".connector.port")
						.toInstance(String.valueOf(port));
			}

			// don't bind any servlets yet, but declare the binding for users to
			// contribute to
			servletBinder(binder);
		}

		@Provides
		public Server createServer(FactoryConfigurationService factoryService, Map<String, Servlet> servlets) {
			return factoryService.factory(ServerFactory.class, configPrefix).createServer(servlets);
		}
	}
}
