package io.bootique.jetty;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.bootique.BQCoreModule;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.env.DefaultEnvironment;
import io.bootique.jetty.command.ServerCommand;
import io.bootique.jetty.server.MappedFilterFactory;
import io.bootique.jetty.server.MappedServletFactory;
import io.bootique.jetty.server.ServerFactory;
import io.bootique.jetty.servlet.DefaultServletEnvironment;
import io.bootique.jetty.servlet.ServletEnvironment;
import io.bootique.log.BootLogger;
import io.bootique.shutdown.ShutdownManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static java.util.Arrays.asList;

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
	 * Adds a servlet for serving static resources for a given URL. The actual
	 * servlet used is Jetty <a href=
	 * "http://download.eclipse.org/jetty/9.3.7.v20160115/apidocs/org/eclipse/jetty/servlet/DefaultServlet.html">
	 * DefaultServlet</a>, and it can be configured further via servlet
	 * parameters. Static resources will be resolved relative to ServerFactory's
	 * "staticResourceBase" , with URL path used to locate a subfolder, unless
	 * servlet-specific configuration is explicitly provided.
	 * 
	 * @since 0.15
	 * @param binder
	 *            DI binder.
	 * @param name
	 *            servlet name that can be referenced in YAML to pass
	 *            parameters.
	 * @param urlPatterns
	 *            url patterns
	 * @see <a href=
	 *      "http://download.eclipse.org/jetty/9.3.7.v20160115/apidocs/org/eclipse/jetty/servlet/DefaultServlet.html">
	 *      DefaultServlet</a>.
	 */
	public static void contributeStaticServlet(Binder binder, String name, String... urlPatterns) {

		Set<String> patternsSet = urlPatterns != null ? new HashSet<>(asList(urlPatterns)) : Collections.emptySet();

		DefaultServlet servlet = new DefaultServlet();
		MappedServlet<DefaultServlet> mappedServlet = new MappedServlet<>(servlet, patternsSet, name);
		contributeMappedServlets(binder).addBinding().toInstance(mappedServlet);
	}

	/**
	 * Adds a default servlet to Jetty, as specified in servlet spec. Equivalent
	 * to 'contributeStaticServlet(binder, "/", "default")'.
	 * 
	 * @since 0.15
	 * @param binder
	 *            DI binder.
	 */
	public static void contributeDefaultServlet(Binder binder) {
		contributeStaticServlet(binder, "default", "/");
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

		// make Jetty less verbose ..
		BQCoreModule.contributeLogLevels(binder).addBinding("org.eclipse.jetty").toInstance(Level.INFO);
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
