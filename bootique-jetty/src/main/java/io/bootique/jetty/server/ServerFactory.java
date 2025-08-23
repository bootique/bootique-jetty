/**
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * “License”); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jetty.server;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.jetty.MappedFilter;
import io.bootique.jetty.MappedListener;
import io.bootique.jetty.MappedServlet;
import io.bootique.jetty.connector.ConnectorFactory;
import io.bootique.jetty.connector.HttpConnectorFactory;
import io.bootique.jetty.request.RequestMDCItem;
import io.bootique.jetty.request.RequestMDCManager;
import io.bootique.resource.FolderResourceFactory;
import io.bootique.shutdown.ShutdownManager;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import org.eclipse.jetty.rewrite.handler.CompactPathRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@BQConfig("Configures embedded Jetty server, including servlet spec objects, web server root location, connectors, " +
        "thread pool parameters, etc.")
public class ServerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerFactory.class);

    private final Set<Servlet> diServlets;
    private final Set<MappedServlet> mappedServlets;
    private final Set<Filter> diFilters;
    private final Set<MappedFilter> mappedFilters;
    private final Set<EventListener> listeners;
    private final Set<MappedListener> mappedListeners;
    private final Set<ServletContextHandlerExtender> contextHandlerExtenders;
    private final Map<String, RequestMDCItem> mdcItems;
    private final ShutdownManager shutdownManager;

    protected List<ConnectorFactory> connectors;
    protected String context;
    protected int idleThreadTimeout;
    protected Map<String, FilterFactory> filters;
    protected int maxThreads;
    protected int minThreads;

    @Deprecated(forRemoval = true)
    protected int maxQueuedRequests;
    protected Map<String, ServletFactory> servlets;
    protected boolean sessions;
    private Map<String, String> params;
    private FolderResourceFactory staticResourceBase;
    private boolean compression;
    private boolean compactPath;
    // defined as "int" in Jetty, so we should not exceed max int
    private int maxFormContentSize;
    private int maxFormKeys;

    /**
     * Maintains a mapping between erroneous response's Status Code and the page (URL) which will be used to handle it further.
     */
    private Map<Integer, String> errorPages;

    @Inject
    public ServerFactory(
            Set<Servlet> diServlets,
            Set<MappedServlet> mappedServlets,
            Set<Filter> diFilters,
            Set<MappedFilter> mappedFilters,
            Set<EventListener> listeners,
            Set<MappedListener> mappedListeners,
            Set<ServletContextHandlerExtender> contextHandlerExtenders,
            Map<String, RequestMDCItem> mdcItems,
            ShutdownManager shutdownManager) {

        this.diServlets = diServlets;
        this.mappedServlets = mappedServlets;
        this.diFilters = diFilters;
        this.mappedFilters = mappedFilters;
        this.listeners = listeners;
        this.mappedListeners = mappedListeners;
        this.contextHandlerExtenders = contextHandlerExtenders;
        this.mdcItems = mdcItems;
        this.shutdownManager = shutdownManager;

        this.minThreads = 4;
        this.maxThreads = 200;
        this.maxQueuedRequests = 1024;
        this.idleThreadTimeout = 60000;
        this.sessions = true;
        this.compression = true;
    }

    /**
     * @since 3.0
     */
    public ServerHolder createServerHolder() {

        String context = resolveContext();

        ThreadPool threadPool = createThreadPool();
        ServletContextHandler contextHandler = createContextHandler(
                context,
                resolveServlets(),
                resolveFilters(),
                resolveListeners());

        // TODO: Using our own port of deprecated Jetty noop symlink alias checker until we decide how to implement
        //  https://github.com/bootique/bootique-jetty/issues/114
        contextHandler.setAliasChecks(List.of(new AllowSymLinkAliasChecker()));

        Server server = new Server(threadPool);
        server.setStopAtShutdown(true);

        // Jetty 10 and 11 implement Graceful class that handles shutdown with timeout. Compared to Jetty 9 the actual
        // shutdown time of a test server increased to between 2 and 3 seconds. It makes tests with Jetty very slow.
        // This may actually be a bug in Jetty, as even when the timeout is 0, and Graceful is bypassed, it still
        // stops its components explicitly (e.g. servlets receive their "destroy" event). So for now leaving it at 0.
        server.setStopTimeout(0);

        // postconfig *after* the handler is associated with the Server. Some extensions like WebSocket require access
        // to the handler's Server
        postConfigHandler(contextHandler, contextHandlerExtenders);

        if (maxFormContentSize > 0) {
            server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", maxFormContentSize);
            contextHandler.setMaxFormContentSize(maxFormContentSize);
        }

        if (maxFormKeys > 0) {
            server.setAttribute("org.eclipse.jetty.server.Request.maxFormKeys", maxFormKeys);
            contextHandler.setMaxFormKeys(maxFormKeys);
        }

        createRequestLog(server);

        server.setHandler(wrapContextHandler(contextHandler));

        Collection<ConnectorFactory> connectorFactories = connectorFactories(server);
        Collection<ConnectorHolder> connectorHolders = new ArrayList<>(2);

        if (connectorFactories.isEmpty()) {
            LOGGER.warn("Jetty starts with no connectors configured. Is that expected?");
        } else {
            connectorFactories.forEach(cf -> {
                NetworkConnector connector = cf.createConnector(server);
                server.addConnector(connector);
                connectorHolders.add(new ConnectorHolder(connector));
            });
        }

        ServerHolder serverHolder = new ServerHolder(server, context, connectorHolders);
        server.addEventListener(new ServerLifecycleLogger(serverHolder));
        return shutdownManager.onShutdown(serverHolder, ServerHolder::stop);
    }

    protected void postConfigHandler(ServletContextHandler handler, Set<ServletContextHandlerExtender> contextHandlerExtenders) {
        contextHandlerExtenders.forEach(c -> c.onHandlerInstalled(handler));
    }

    protected ServletContextHandler createContextHandler(
            String context,
            Set<MappedServlet> servlets,
            Set<MappedFilter> filters,
            Set<MappedListener> listeners) {

        int options = 0;

        if (sessions) {
            options |= ServletContextHandler.SESSIONS;
        }

        ServletContextHandler handler = new ServletContextHandler(options);
        handler.setContextPath(context);

        if (params != null) {
            params.forEach(handler::setInitParameter);
        }

        if (staticResourceBase != null) {
            handler.setBaseResourceAsString(staticResourceBase.getUrl().toExternalForm());
        }

        if (compression) {
            handler.insertHandler(createGzipHandler());
        }

        if (errorPages != null) {
            ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
            errorPages.forEach(errorHandler::addErrorPage);
            handler.setErrorHandler(errorHandler);
        }

        installListeners(handler, listeners);
        installServlets(handler, servlets);
        installFilters(handler, filters);

        return handler;
    }

    protected Handler wrapContextHandler(ContextHandler handler) {
        Handler h2 = compactPath ? createCompactPathHandler(handler) : handler;
        return new RequestMDCManager(h2, mdcItems);
    }

    protected Handler createCompactPathHandler(Handler handler) {
        RewriteHandler compactPathHandler = new RewriteHandler();

        // TODO: no tests that this actually does something
        compactPathHandler.addRule(new CompactPathRule());
        compactPathHandler.setHandler(handler);

        return compactPathHandler;
    }


    protected GzipHandler createGzipHandler() {
        return new GzipHandler();
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

    protected void installListeners(ServletContextHandler handler, Set<MappedListener> listeners) {

        if (listeners.isEmpty()) {
            return;
        }

        sortedListeners(listeners).forEach(listener -> {
            LOGGER.info("Adding listener {}", listener.getListener().getClass().getName());
            handler.addEventListener(listener.getListener());
        });
    }

    private List<MappedFilter> sortedFilters(Set<MappedFilter> unsorted) {
        List<MappedFilter> sorted = new ArrayList<>(unsorted);

        sorted.sort(Comparator.comparing(MappedFilter::getOrder));
        return sorted;
    }

    private List<MappedListener> sortedListeners(Set<MappedListener> unsorted) {
        List<MappedListener> sorted = new ArrayList<>(unsorted);

        sorted.sort(Comparator.comparing(MappedListener::getOrder));
        return sorted;
    }

    protected Collection<ConnectorFactory> connectorFactories(Server server) {
        Collection<ConnectorFactory> connectorFactories = new ArrayList<>();

        if (this.connectors != null) {
            connectorFactories.addAll(this.connectors);
        }

        // add default connector if none are configured
        if (connectorFactories.isEmpty()) {
            connectorFactories.add(new HttpConnectorFactory());
        }

        return connectorFactories;
    }

    protected QueuedThreadPool createThreadPool() {
        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleThreadTimeout);
        threadPool.setName("bootique-http");
        return threadPool;
    }

    protected void createRequestLog(Server server) {

        Logger logger = LoggerFactory.getLogger(RequestLogger.class);
        if (logger.isInfoEnabled()) {
            server.setRequestLog(new RequestLogger());
        }
    }

    /**
     * @return a List of server connectors, each listening on its own unique port.
     */
    public List<ConnectorFactory> getConnectors() {
        return connectors;
    }

    /**
     * Sets a list of connector factories for this server. Each connectors would listen on its own unique port.
     *
     * @param connectors a list of preconfigured connector factories.
     */
    @BQConfigProperty("A list of objects specifying properties of the server network connectors.")
    public void setConnectors(List<ConnectorFactory> connectors) {
        this.connectors = connectors;
    }

    @BQConfigProperty("A map of servlet configurations by servlet name. ")
    public void setServlets(Map<String, ServletFactory> servlets) {
        this.servlets = servlets;
    }

    @BQConfigProperty("A map of servlet Filter configurations by filter name.")
    public void setFilters(Map<String, FilterFactory> filters) {
        this.filters = filters;
    }

    /**
     * @return web application context path.
     */
    public String getContext() {
        return context;
    }

    @BQConfigProperty("Web application context path. The default is '/'.")
    public void setContext(String context) {
        this.context = context;
    }

    protected String resolveContext() {
        if (context == null) {
            return "/";
        }

        // context must start with a slash and must not end with a slash...
        // fix sloppy configuration on the fly
        String c1 = context.startsWith("/") ? context : "/" + context;
        String c2 = (c1.length() > 1 && c1.endsWith("/")) ? c1.substring(0, c1.length() - 1) : c1;
        return c2;
    }

    static int maxOrder(Set<MappedFilter> mappedFilters) {
        return mappedFilters.stream().map(MappedFilter::getOrder).max(Integer::compare).orElse(0);
    }

    private Set<MappedServlet> resolveServlets() {
        if (diServlets.isEmpty()) {
            return mappedServlets;
        }

        Set<MappedServlet> mappedServletsClone = new HashSet<>(mappedServlets);
        MappedServletFactory mappedServletFactory = new MappedServletFactory();
        diServlets.forEach(servlet -> mappedServletsClone.add(mappedServletFactory.toMappedServlet(servlet)));
        return mappedServletsClone;
    }

    private Set<MappedFilter> resolveFilters() {
        if (diFilters.isEmpty()) {
            return mappedFilters;
        }

        // place annotated filters after the last explicit filter.. In any event
        // the actual ordering is unpredictable (depends on the set iteration
        // order).
        AtomicInteger order = new AtomicInteger(maxOrder(mappedFilters) + 1);

        Set<MappedFilter> mappedFiltersClone = new HashSet<>(mappedFilters);
        MappedFilterFactory mappedFilterFactory = new MappedFilterFactory();
        diFilters.forEach(
                filter -> mappedFiltersClone.add(mappedFilterFactory.toMappedFilter(filter, order.getAndIncrement())));

        return mappedFiltersClone;
    }

    private Set<MappedListener> resolveListeners() {
        if (listeners.isEmpty()) {
            return mappedListeners;
        }

        Set<MappedListener> mappedListenersClone = new HashSet<>(mappedListeners);

        //  Integer.MAX_VALUE means placing bare unordered listeners after (== inside) mapped listeners
        listeners.forEach(
                listener -> mappedListenersClone.add(new MappedListener<>(listener, Integer.MAX_VALUE)));

        return mappedListenersClone;
    }

    /**
     * @return a period in milliseconds specifying how long it takes until an idle thread is terminated.
     */
    public int getIdleThreadTimeout() {
        return idleThreadTimeout;
    }

    @BQConfigProperty("A period in milliseconds specifying how long until an idle thread is terminated. ")
    public void setIdleThreadTimeout(int idleThreadTimeout) {
        this.idleThreadTimeout = idleThreadTimeout;
    }

    /**
     * @return a maximum number of requests to queue if the thread pool is busy.
     * @deprecated ignored, as bounded queue is no longer recommended by Jetty
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public int getMaxQueuedRequests() {
        return maxQueuedRequests;
    }

    /**
     * @deprecated ignored, as bounded queue is no longer recommended by Jetty
     */
    @BQConfigProperty("""
            ** Deprecated and ignored. Maximum number of requests to queue if the thread pool is busy. If this number
            is exceeded, the server will start dropping requests.""")
    @Deprecated(since = "4.0.0", forRemoval = true)
    public void setMaxQueuedRequests(int maxQueuedRequests) {
        LOGGER.warn("'jetty.maxQueuedRequests' property is deprecated and ignored");
        this.maxQueuedRequests = maxQueuedRequests;
    }

    /**
     * @return a maximum number of request processing threads in the pool.
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    @BQConfigProperty("Maximum number of request processing threads in the pool.")
    public void setMaxThreads(int maxConnectorThreads) {
        this.maxThreads = maxConnectorThreads;
    }

    /**
     * @return an initial number of request processing threads in the pool.
     */
    public int getMinThreads() {
        return minThreads;
    }

    @BQConfigProperty("Minimal number of request processing threads in the pool.")
    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }

    /**
     * @return a map of arbitrary key/value parameters that are used as "init" parameters of the ServletContext.
     */
    public Map<String, String> getParams() {
        return params != null ? Collections.unmodifiableMap(params) : Collections.emptyMap();
    }

    /**
     * @param params a map of context init parameters.
     */
    @BQConfigProperty("A map of application-specific key/value parameters that are used as \"init\" parameters of the " +
            "ServletContext.")
    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    /**
     * @return a boolean specifying whether servlet sessions should be supported
     * by Jetty.
     */
    public boolean isSessions() {
        return sessions;
    }

    @BQConfigProperty("A boolean specifying whether servlet sessions should be supported by Jetty. The default is 'true'")
    public void setSessions(boolean sessions) {
        this.sessions = sessions;
    }

    /**
     * @return a base location for resources of the Jetty context.
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
     * For security reasons, this has to be set explicitly. There's no default.
     *
     * @param staticResourceBase A base location for resources of the Jetty context, that can
     *                           be a file path or a URL, as well as a special URL starting
     *                           with "classpath:".
     * @see MappedServlet#ofStatic(String...)
     */
    @BQConfigProperty("Defines a base location for resources of the Jetty context. It can be a filesystem path, a URL " +
            "or a special \"classpath:\" URL (giving the ability to bundle resources in the app, not unlike a JavaEE " +
            ".war file). This setting only makes sense when some form of \"default\" servlet is in use, that will be " +
            "responsible for serving static resources. See JettyModule.contributeStaticServlet(..) or " +
            "JettyModule.contributeDefaultServlet(..). ")
    public void setStaticResourceBase(FolderResourceFactory staticResourceBase) {
        this.staticResourceBase = staticResourceBase;
    }

    /**
     * @return whether content compression is supported.
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
     */
    @BQConfigProperty("A boolean specifying whether gzip compression should be supported. When enabled " +
            "responses will be compressed if a client indicates it supports compression via " +
            "\"Accept-Encoding: gzip\" header. Default value is 'true'.")
    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    /**
     * Compact URLs with multiple '/'s with a single '/'.
     *
     * @param compactPath Compact URLs with multiple '/'s with a single '/'. Default value is 'false'
     */
    @BQConfigProperty("Replaces multiple '/'s with a single '/' in URL. Default value is 'false'.")
    public void setCompactPath(boolean compactPath) {
        this.compactPath = compactPath;
    }

    /**
     * Sets the maximum size of submitted forms in bytes. Default is 200000 (~195K).
     *
     * @param maxFormContentSize maximum size of submitted forms in bytes. Default is 200000 (~195K)
     */
    @BQConfigProperty("Maximum size of submitted forms in bytes. Default is 200000 (~195K).")
    public void setMaxFormContentSize(int maxFormContentSize) {
        this.maxFormContentSize = maxFormContentSize;
    }

    /**
     * Sets the maximum number of form fields. Default is 1000.
     *
     * @param maxFormKeys maximum number of form fields. Default is 1000.
     */
    @BQConfigProperty("Maximum number of form fields. Default is 1000.")
    public void setMaxFormKeys(int maxFormKeys) {
        this.maxFormKeys = maxFormKeys;
    }

    /**
     * @return a potentially null map of error pages configuration.
     */
    public Map<Integer, String> getErrorPages() {
        return errorPages;
    }

    /**
     * Sets mappings between HTTP status codes and corresponding pages which will be returned to the user instead.
     *
     * @param errorPages map where keys are HTTP status codes and values are page URLs which will be used to handle them
     */
    @BQConfigProperty("A map specifying a mapping between HTTP status codes and pages (URLs) which will be used as their handlers. If no mapping is specified then standard error handler is used.")
    public void setErrorPages(Map<Integer, String> errorPages) {
        this.errorPages = errorPages;
    }
}
