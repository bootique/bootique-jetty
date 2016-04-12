package com.nhl.bootique.jetty;

import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.server.Server;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.nhl.bootique.BQCoreModule;
import com.nhl.bootique.ConfigModule;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.env.DefaultEnvironment;
import com.nhl.bootique.jetty.command.ServerCommand;
import com.nhl.bootique.jetty.server.MappedFilterFactory;
import com.nhl.bootique.jetty.server.MappedServletFactory;
import com.nhl.bootique.jetty.server.ServerFactory;
import com.nhl.bootique.jetty.servlet.DefaultServletEnvironment;
import com.nhl.bootique.jetty.servlet.ServletEnvironment;
import com.nhl.bootique.log.BootLogger;
import com.nhl.bootique.shutdown.ShutdownManager;

public class JettyModule extends ConfigModule {

	/**
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.14
	 * @return returns a {@link Multibinder} for servlets.
	 */
	public static Multibinder<MappedServlet> contributeMappedServlets(Binder binder) {
		return Multibinder.newSetBinder(binder, MappedServlet.class);
	}

	/**
	 * Returns a {@link Multibinder} for container servlets. Servlets must be
	 * annotated with {@link WebServlet}. Otherwise they should be mapped via
	 * {@link #contributeMappedServlets(Binder)}.
	 * 
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.14
	 * @return returns a {@link Multibinder} for servlets.
	 */
	public static Multibinder<Servlet> contributeServlets(Binder binder) {
		return Multibinder.newSetBinder(binder, Servlet.class);
	}

	/**
	 * Returns a {@link Multibinder} for servlet filters. Filters must be
	 * annotated with {@link WebFilter}. Otherwise they should be mapped via
	 * {@link #contributeMappedFilters(Binder)}, where you can explicitly
	 * specify URL patterns, etc.
	 * 
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.14
	 * @return returns a {@link Multibinder} for container filters.
	 */
	public static Multibinder<Filter> contributeFilters(Binder binder) {
		return Multibinder.newSetBinder(binder, Filter.class);
	}

	/**
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.14
	 * @return returns a {@link Multibinder} for servlet filters.
	 */
	public static Multibinder<MappedFilter> contributeMappedFilters(Binder binder) {
		return Multibinder.newSetBinder(binder, MappedFilter.class);
	}

	/**
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.12
	 * @return returns a {@link Multibinder} for web listeners.
	 */
	public static Multibinder<EventListener> contributeListeners(Binder binder) {
		return Multibinder.newSetBinder(binder, EventListener.class);
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

		BQCoreModule.contributeCommands(binder).addBinding().to(ServerCommand.class).in(Singleton.class);

		if (context != null) {
			BQCoreModule.contributeProperties(binder)
					.addBinding(DefaultEnvironment.FRAMEWORK_PROPERTIES_PREFIX + "." + configPrefix + ".context")
					.toInstance(context);
		}

		if (port > 0) {
			BQCoreModule.contributeProperties(binder)
					.addBinding(DefaultEnvironment.FRAMEWORK_PROPERTIES_PREFIX + "." + configPrefix + ".connector.port")
					.toInstance(String.valueOf(port));
		}

		// trigger extension points creation

		JettyModule.contributeServlets(binder);
		JettyModule.contributeFilters(binder);

		JettyModule.contributeMappedServlets(binder);
		JettyModule.contributeMappedFilters(binder);

		JettyModule.contributeListeners(binder);

		// register default listeners
		JettyModule.contributeListeners(binder).addBinding().to(DefaultServletEnvironment.class);
	}

	@Singleton
	@Provides
	ServletEnvironment createStateTracker(DefaultServletEnvironment stateImpl) {
		return stateImpl;
	}

	@Singleton
	@Provides
	DefaultServletEnvironment createStateTrackerImpl() {
		return new DefaultServletEnvironment();
	}

	@Singleton
	@Provides
	Server createServer(ServerFactory factory, Set<Servlet> servlets, Set<MappedServlet> mappedServlets,
			Set<Filter> filters, Set<MappedFilter> mappedFilters, Set<EventListener> listeners, BootLogger bootLogger,
			ShutdownManager shutdownManager) {

		Server server = factory.createServer(allServlets(servlets, mappedServlets), allFilters(filters, mappedFilters),
				listeners);

		shutdownManager.addShutdownHook(() -> {
			bootLogger.trace(() -> "stopping Jetty...");
			server.stop();
		});

		return server;
	}

	private Set<MappedServlet> allServlets(Set<Servlet> servlets, Set<MappedServlet> mappedServlets) {
		if (servlets.isEmpty()) {
			return mappedServlets;
		}

		Set<MappedServlet> mappedServletsClone = new HashSet<>(mappedServlets);
		MappedServletFactory mappedServletFactory = new MappedServletFactory();
		servlets.forEach(servlet -> mappedServletsClone.add(mappedServletFactory.toMappedServlet(servlet)));
		return mappedServletsClone;
	}

	private Set<MappedFilter> allFilters(Set<Filter> filters, Set<MappedFilter> mappedFilters) {
		if (filters.isEmpty()) {
			return mappedFilters;
		}

		// place annotated filters after the last explicit filter.. In any event
		// the actual ordering is unpredictable (depends on the set iteration
		// order).
		AtomicInteger order = new AtomicInteger(maxOrder(mappedFilters) + 1);

		Set<MappedFilter> mappeFiltersClone = new HashSet<>(mappedFilters);
		MappedFilterFactory mappedFilterFactory = new MappedFilterFactory();
		filters.forEach(
				filter -> mappeFiltersClone.add(mappedFilterFactory.toMappedFilter(filter, order.getAndIncrement())));

		return mappeFiltersClone;
	}

	static int maxOrder(Set<MappedFilter> mappedFilters) {
		return mappedFilters.stream().map(MappedFilter::getOrder).max(Integer::compare).orElse(0);
	}

	@Singleton
	@Provides
	ServerFactory createServerFactory(ConfigurationFactory configFactory) {
		return configFactory.config(ServerFactory.class, configPrefix);
	}
}
