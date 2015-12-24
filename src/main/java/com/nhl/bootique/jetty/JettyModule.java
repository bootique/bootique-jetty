package com.nhl.bootique.jetty;

import java.util.Map;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Server;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.nhl.bootique.BQBinder;
import com.nhl.bootique.ConfigModule;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.env.DefaultEnvironment;
import com.nhl.bootique.jetty.command.ServerCommand;
import com.nhl.bootique.jetty.server.ServerFactory;

public class JettyModule extends ConfigModule {

	/**
	 * Utility method for other modules to contribute servlets to Jetty.
	 */
	public static MapBinder<String, Servlet> servletBinder(Binder binder) {
		return MapBinder.newMapBinder(binder, String.class, Servlet.class);
	}

	private String context;
	private int port;

	public JettyModule(String configPrefix) {
		super(configPrefix);
	}

	public JettyModule() {
	}

	public JettyModule context(String context) {
		this.context = context;
		return this;
	}

	public JettyModule port(int port) {
		this.port = port;
		return this;
	}

	@Override
	public void configure(Binder binder) {

		BQBinder bqContribs = BQBinder.contributeTo(binder);
		bqContribs.commandTypes(ServerCommand.class);

		if (context != null) {
			bqContribs.property(DefaultEnvironment.FRAMEWORK_PROPERTIES_PREFIX + "." + configPrefix + ".context",
					context);
		}

		if (port > 0) {
			bqContribs.property(DefaultEnvironment.FRAMEWORK_PROPERTIES_PREFIX + "." + configPrefix + ".connector.port",
					String.valueOf(port));
		}

		// don't bind any servlets yet, but declare the binding for users to
		// contribute to
		servletBinder(binder);
	}

	@Provides
	public Server createServer(ConfigurationFactory configFactory, Map<String, Servlet> servlets) {
		return configFactory.config(ServerFactory.class, configPrefix).createServer(servlets);
	}

}
