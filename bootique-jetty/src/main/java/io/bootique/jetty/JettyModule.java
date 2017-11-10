package io.bootique.jetty;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.bootique.BQCoreModule;
import io.bootique.BQCoreModuleExtender;
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
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class JettyModule extends ConfigModule {

    private String context;
    private int port;

    public JettyModule(String configPrefix) {
        super(configPrefix);
    }

    public JettyModule() {
    }

    /**
     * Returns an instance of {@link JettyModuleExtender} used by downstream modules to load custom extensions of
     * services declared in the JettyModule. Should be invoked from a downstream Module's "configure" method.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link JettyModuleExtender} that can be used to load Jetty custom extensions.
     * @since 0.20
     */
    public static JettyModuleExtender extend(Binder binder) {
        return new JettyModuleExtender(binder);
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for servlets.
     * @since 0.14
     * @deprecated since 0.20 call {@link #extend(Binder)} and then call
     * {@link JettyModuleExtender#addMappedServlet(MappedServlet)} or similar methods.
     */
    @Deprecated
    public static Multibinder<MappedServlet> contributeMappedServlets(Binder binder) {
        return extend(binder).contributeMappedServlets();
    }

    /**
     * Returns a {@link Multibinder} for container servlets. Servlets must be
     * annotated with {@link WebServlet}. Otherwise they should be mapped via
     * {@link #contributeMappedServlets(Binder)}.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for servlets.
     * @since 0.14
     * @deprecated since 0.20 call {@link #extend(Binder)} and then call {@link JettyModuleExtender#addServlet(Class)}
     * or similar methods.
     */
    @Deprecated
    public static Multibinder<Servlet> contributeServlets(Binder binder) {
        return extend(binder).contributeServlets();
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
     * @param binder      DI binder.
     * @param name        servlet name that can be referenced in YAML to pass
     *                    parameters.
     * @param urlPatterns url patterns
     * @see <a href=
     * "http://download.eclipse.org/jetty/9.3.7.v20160115/apidocs/org/eclipse/jetty/servlet/DefaultServlet.html">
     * DefaultServlet</a>.
     * @since 0.15
     * @deprecated since 0.20 call {@link #extend(Binder)} and then call
     * {@link JettyModuleExtender#addStaticServlet(String, String...)}.
     */
    @Deprecated
    public static void contributeStaticServlet(Binder binder, String name, String... urlPatterns) {
        extend(binder).addServlet(new DefaultServlet(), name, urlPatterns);
    }

    /**
     * Adds a default servlet to Jetty, as specified in servlet spec. Equivalent
     * to 'contributeStaticServlet(binder, "/", "default")'.
     *
     * @param binder DI binder.
     * @since 0.15
     * @deprecated since 0.20 call {@link #extend(Binder)} and then call
     * {@link JettyModuleExtender#useDefaultServlet()}.
     */
    @Deprecated
    public static void contributeDefaultServlet(Binder binder) {
        extend(binder).useDefaultServlet();
    }

    /**
     * Returns a {@link Multibinder} for servlet filters. Filters must be
     * annotated with {@link WebFilter}. Otherwise they should be mapped via
     * {@link #contributeMappedFilters(Binder)}, where you can explicitly
     * specify URL patterns, etc.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for container filters.
     * @since 0.14
     * @deprecated since 0.20 call {@link #extend(Binder)} and then call {@link JettyModuleExtender#addFilter(Class)}
     * or similar methods.
     */
    @Deprecated
    public static Multibinder<Filter> contributeFilters(Binder binder) {
        return extend(binder).contributeFilters();
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for servlet filters.
     * @since 0.14
     * @deprecated since 0.20 call {@link #extend(Binder)} and then call
     * {@link JettyModuleExtender#addMappedFilter(MappedFilter)} or similar methods.
     */
    @Deprecated
    public static Multibinder<MappedFilter> contributeMappedFilters(Binder binder) {
        return extend(binder).contributeMappedFilters();
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for web listeners.
     * @since 0.12
     * @deprecated since 0.20 call {@link #extend(Binder)} and then call
     * {@link JettyModuleExtender#addListener(Class)}or similar methods.
     */
    @Deprecated
    public static Multibinder<EventListener> contributeListeners(Binder binder) {
        return extend(binder).contributeListeners();
    }

    static int maxOrder(Set<MappedFilter> mappedFilters) {
        return mappedFilters.stream().map(MappedFilter::getOrder).max(Integer::compare).orElse(0);
    }

    // TODO: deprecate
    public JettyModule context(String context) {
        this.context = context;
        return this;
    }

    // TODO: deprecate
    public JettyModule port(int port) {
        this.port = port;
        return this;
    }

    @Override
    public void configure(Binder binder) {

        BQCoreModuleExtender coreExtender = BQCoreModule
                .extend(binder)
                .addCommand(ServerCommand.class)
                // make Jetty less verbose ..
                .setLogLevel("org.eclipse.jetty", Level.INFO);

        if (context != null) {
            coreExtender.setProperty(
                    DefaultEnvironment.FRAMEWORK_PROPERTIES_PREFIX + "." + configPrefix + ".context",
                    context);
        }

        if (port > 0) {
            coreExtender.setProperty(
                    DefaultEnvironment.FRAMEWORK_PROPERTIES_PREFIX + "." + configPrefix + ".connector.port",
                    String.valueOf(port)
            );
        }

        // trigger extension points creation and init defaults
        JettyModule.extend(binder)
                .initAllExtensions()
                .addListener(DefaultServletEnvironment.class);
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
    Server createServer(ServerFactory factory,
                        Set<Servlet> servlets,
                        Set<MappedServlet> mappedServlets,
                        Set<Filter> filters,
                        Set<MappedFilter> mappedFilters,
                        Set<EventListener> listeners,
                        Set<MappedListener> mappedListeners,
                        BootLogger bootLogger,
                        ShutdownManager shutdownManager) {

        Server server = factory.createServer(
                allServlets(servlets, mappedServlets),
                allFilters(filters, mappedFilters),
                allListeners(listeners, mappedListeners));

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

    private Set<MappedListener> allListeners(Set<EventListener> listeners, Set<MappedListener> mappedListeners) {
        if (listeners.isEmpty()) {
            return mappedListeners;
        }

        Set<MappedListener> mappedListenersClone = new HashSet<>(mappedListeners);

        //  Integer.MAX_VALUE means placing bare unordered listeners after (== inside) mapped listeners
        listeners.forEach(
                listener -> mappedListenersClone.add(new MappedListener<>(listener, Integer.MAX_VALUE)));

        return mappedListenersClone;
    }

    @Singleton
    @Provides
    ServerFactory createServerFactory(ConfigurationFactory configFactory) {
        return configFactory.config(ServerFactory.class, configPrefix);
    }
}
