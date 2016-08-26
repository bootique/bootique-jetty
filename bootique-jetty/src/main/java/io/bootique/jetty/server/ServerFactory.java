package io.bootique.jetty.server;

import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedFilter;
import io.bootique.jetty.MappedServlet;
import io.bootique.jetty.connector.ConnectorFactory;
import io.bootique.jetty.connector.HttpConnectorFactory;
import io.bootique.resource.FolderResourceFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class ServerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerFactory.class);

    @Deprecated
    protected ConnectorFactory connector;
    protected List<ConnectorFactory> connectors;
    protected String context;
    protected int idleThreadTimeout;
    protected Map<String, FilterFactory> filters;
    protected int maxThreads;
    protected int minThreads;
    protected int maxQueuedRequests;
    protected Map<String, ServletFactory> servlets;
    protected boolean sessions;
    private Map<String, String> params;
    private FolderResourceFactory staticResourceBase;
    private boolean compression;

    public ServerFactory() {
        this.context = "/";
        this.minThreads = 4;
        this.maxThreads = 1024;
        this.maxQueuedRequests = 1024;
        this.idleThreadTimeout = 60000;
        this.sessions = true;
        this.compression = true;
    }

    public Server createServer(Set<MappedServlet> servlets, Set<MappedFilter> filters, Set<EventListener> listeners) {

        ThreadPool threadPool = createThreadPool();
        Server server = new Server(threadPool);
        server.setStopAtShutdown(true);
        server.setStopTimeout(1000L);
        server.setHandler(createHandler(servlets, filters, listeners));

        createRequestLog(server);

        Collection<ConnectorFactory> connectorFactories = connectorFactories(server);

        Collection<ConnectorDescriptor> connectorDescriptors = new ArrayList<>(2);

        if (connectorFactories.isEmpty()) {
            LOGGER.warn("Jetty starts with no connectors configured. Is that expected?");
        } else {
            connectorFactories.forEach(cf -> {
                NetworkConnector connector = cf.createConnector(server);
                server.addConnector(connector);
                connectorDescriptors.add(new ConnectorDescriptor(connector));
            });
        }

        server.addLifeCycleListener(new ServerLifecycleLogger(connectorDescriptors, context));
        return server;
    }

    protected Handler createHandler(Set<MappedServlet> servlets, Set<MappedFilter> filters,
                                    Set<EventListener> listeners) {

        int options = 0;

        if (sessions) {
            options |= ServletContextHandler.SESSIONS;
        }

        ServletContextHandler handler = new ServletContextHandler(options);
        handler.setContextPath(context);
        if (params != null) {
            params.forEach((k, v) -> handler.setInitParameter(k, v));
        }

        if (staticResourceBase != null) {
            handler.setResourceBase(staticResourceBase.getUrl().toExternalForm());
        }

        if (compression) {
            handler.setGzipHandler(createGzipHandler());
        }

        installListeners(handler, listeners);
        installServlets(handler, servlets);
        installFilters(handler, filters);

        return handler;
    }

    protected GzipHandler createGzipHandler() {
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setCheckGzExists(false);
        return gzipHandler;
    }

    protected void installServlets(ServletContextHandler handler, Set<MappedServlet> servlets) {
        servlets.forEach(mappedServlet -> getServletFactory(mappedServlet.getName()).createAndAddJettyServlet(handler,
                mappedServlet));
    }

    protected ServletFactory getServletFactory(String name) {
        ServletFactory factory = null;
        if (servlets != null && name != null) {
            factory = servlets.get(name);
        }

        return factory != null ? factory : new ServletFactory();
    }

    protected void installFilters(ServletContextHandler handler, Set<MappedFilter> filters) {
        sortedFilters(filters).forEach(mappedFilter -> getFilterFactory(mappedFilter.getName())
                .createAndAddJettyFilter(handler, mappedFilter));
    }

    protected FilterFactory getFilterFactory(String name) {
        FilterFactory factory = null;
        if (filters != null && name != null) {
            factory = filters.get(name);
        }

        return factory != null ? factory : new FilterFactory();
    }

    protected void installListeners(ServletContextHandler handler, Set<EventListener> listeners) {
        listeners.forEach(listener -> {

            LOGGER.info("Adding listener {}", listener.getClass().getName());
            handler.addEventListener(listener);
        });
    }

    private List<MappedFilter> sortedFilters(Set<MappedFilter> unsorted) {
        List<MappedFilter> sorted = new ArrayList<>(unsorted);

        Collections.sort(sorted, Comparator.comparing(MappedFilter::getOrder));
        return sorted;
    }

    protected Collection<ConnectorFactory> connectorFactories(Server server) {
        Collection<ConnectorFactory> connectorFactories = new ArrayList<>();

        if (this.connector != null) {
            LOGGER.warn("Using deprecated 'connector' property. Consider changing configuration to 'connectors'");
            connectorFactories.add(this.connector);
        }

        if (this.connectors != null) {
            connectorFactories.addAll(this.connectors);
        }

        // add default connector if none are configured
        if(connectorFactories.isEmpty()) {
            connectorFactories.add(new HttpConnectorFactory());
        }

        return connectorFactories;
    }

    protected QueuedThreadPool createThreadPool() {
        BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(minThreads, maxThreads, maxQueuedRequests);
        QueuedThreadPool threadPool = createThreadPool(queue);
        threadPool.setName("bootique-http");

        return threadPool;
    }

    protected QueuedThreadPool createThreadPool(BlockingQueue<Runnable> queue) {
        return new QueuedThreadPool(maxThreads, minThreads, idleThreadTimeout, queue);
    }

    protected void createRequestLog(Server server) {

        Logger logger = LoggerFactory.getLogger(RequestLogger.class);
        if (logger.isInfoEnabled()) {
            server.setRequestLog(new RequestLogger());
        }
    }

    /**
     * @return a List of server connectors, each listening on its own unique port.
     * @since 0.18
     */
    public List<ConnectorFactory> getConnectors() {
        return connectors;
    }

    /**
     * Sets a list of connector factories for this server. Each connectors would listen on its own unique port.
     *
     * @param connectors a list of preconfigured connector factories.
     * @since 0.18
     */
    public void setConnectors(List<ConnectorFactory> connectors) {
        this.connectors = connectors;
    }

    public void setServlets(Map<String, ServletFactory> servlets) {
        this.servlets = servlets;
    }

    public void setFilters(Map<String, FilterFactory> filters) {
        this.filters = filters;
    }

    /**
     * @return an object containing properties of the web connector and acting
     * as connector factory.
     * @since 0.15
     * @deprecated since 0.18 in favor of "connectors" property.
     */
    @Deprecated
    public ConnectorFactory getConnector() {
        return connector;
    }

    /**
     * @param connector connector factory
     * @deprecated since 0.18 in favor of "connectors" property.
     */
    @Deprecated
    public void setConnector(ConnectorFactory connector) {
        this.connector = connector;
    }

    /**
     * @return web application context path.
     * @since 0.15
     */
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    /**
     * @return a period in milliseconds specifying how long it takes until an
     * idle thread is terminated.
     * @since 0.15
     */
    public int getIdleThreadTimeout() {
        return idleThreadTimeout;
    }

    public void setIdleThreadTimeout(int idleThreadTimeout) {
        this.idleThreadTimeout = idleThreadTimeout;
    }

    /**
     * @return a maximum number of requests to queue if the thread pool is busy.
     * @since 0.15
     */
    public int getMaxQueuedRequests() {
        return maxQueuedRequests;
    }

    public void setMaxQueuedRequests(int maxQueuedRequests) {
        this.maxQueuedRequests = maxQueuedRequests;
    }

    /**
     * @return a maximum number of request processing threads in the pool.
     * @since 0.15
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxConnectorThreads) {
        this.maxThreads = maxConnectorThreads;
    }

    /**
     * @return an initial number of request processing threads in the pool.
     * @since 0.15
     */
    public int getMinThreads() {
        return minThreads;
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }

    /**
     * @return a map of arbitrary key/value parameters that are used as "init"
     * parameters of the ServletContext.
     * @since 0.15
     */
    public Map<String, String> getParams() {
        return params != null ? Collections.unmodifiableMap(params) : Collections.emptyMap();
    }

    /**
     * @param params a map of context init parameters.
     * @since 0.13
     */
    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    /**
     * @return a boolean specifying whether servlet sessions should be supported
     * by Jetty.
     * @since 0.15
     */
    public boolean isSessions() {
        return sessions;
    }

    public void setSessions(boolean sessions) {
        this.sessions = sessions;
    }

    /**
     * @return a base location for resources of the Jetty context.
     * @since 0.15
     */
    public FolderResourceFactory getStaticResourceBase() {
        return staticResourceBase;
    }

    /**
     * Sets a base location for resources of the Jetty context. Used by static
     * resource servlets, including the "default" servlet. The value can be a
     * file path or a URL, as well as a special URL starting with "classpath:".
     * <p>
     * It can be optionally overridden by DefaultServlet configuration.
     * <p>
     * For security reasons this has to be set explicitly when "staticResources"
     * is "true". There's no default.
     *
     * @param staticResourceBase A base location for resources of the Jetty context, that can
     *                           be a file path or a URL, as well as a special URL starting
     *                           with "classpath:".
     * @see JettyModule#contributeDefaultServlet(com.google.inject.Binder)
     * @see JettyModule#contributeStaticServlet(com.google.inject.Binder,
     * String, String...)
     * @see <a href=
     * "http://download.eclipse.org/jetty/9.3.7.v20160115/apidocs/org/eclipse/jetty/servlet/DefaultServlet.html">
     * DefaultServlet</a>.
     * @since 0.13
     */
    public void setStaticResourceBase(FolderResourceFactory staticResourceBase) {
        this.staticResourceBase = staticResourceBase;
    }

    /**
     * @return whether content compression is supported.
     * @since 0.15
     */
    public boolean isCompression() {
        return compression;
    }

    /**
     * Sets whether compression whether gzip compression should be supported.
     * When true, responses will be compressed if a client requests it via
     * "Accept-Encoding:" header. Default is true.
     *
     * @param compression whether gzip compression should be supported.
     * @since 0.15
     */
    public void setCompression(boolean compression) {
        this.compression = compression;
    }
}
