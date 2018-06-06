/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.jetty.server;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.jetty.JettyModuleExtender;
import io.bootique.jetty.MappedFilter;
import io.bootique.jetty.MappedListener;
import io.bootique.jetty.MappedServlet;
import io.bootique.jetty.connector.ConnectorFactory;
import io.bootique.jetty.connector.HttpConnectorFactory;
import io.bootique.resource.FolderResourceFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

@BQConfig("Configures embedded Jetty server, including servlet spec objects, web server root location, connectors, " +
        "thread pool parameters, etc.")
public class ServerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerFactory.class);

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
    private boolean compactPath;
    // defined as "int" in Jetty, so we should not exceed max int
    private int maxFormContentSize;
    private int maxFormKeys;

    /**
     * Maintains a mapping between erroneous response's Status Code and the page (URL) which will be used to handle it further.
     */
    private Map<Integer, String> errorPages;

    public ServerFactory() {
        this.context = "/";
        this.minThreads = 4;
        this.maxThreads = 1024;
        this.maxQueuedRequests = 1024;
        this.idleThreadTimeout = 60000;
        this.sessions = true;
        this.compression = true;
    }

    public Server createServer(Set<MappedServlet> servlets, Set<MappedFilter> filters, Set<MappedListener> listeners) {

        ThreadPool threadPool = createThreadPool();
        Server server = new Server(threadPool);
        server.setStopAtShutdown(true);
        server.setStopTimeout(1000L);
        server.setHandler(createHandler(servlets, filters, listeners));

        if (maxFormContentSize > 0) {
            server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", maxFormContentSize);
        }

        if (maxFormKeys > 0) {
            server.setAttribute("org.eclipse.jetty.server.Request.maxFormKeys", maxFormKeys);
        }

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

    protected Handler createHandler(Set<MappedServlet> servlets,
                                    Set<MappedFilter> filters,
                                    Set<MappedListener> listeners) {

        int options = 0;

        if (sessions) {
            options |= ServletContextHandler.SESSIONS;
        }

        ServletContextHandler handler = new ServletContextHandler(options);
        handler.setContextPath(context);
        handler.setCompactPath(compactPath);
        if (params != null) {
            params.forEach((k, v) -> handler.setInitParameter(k, v));
        }

        if (staticResourceBase != null) {
            handler.setResourceBase(staticResourceBase.getUrl().toExternalForm());
        }

        if (compression) {
            handler.setGzipHandler(createGzipHandler());
        }

        if (errorPages != null) {
            ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
            errorPages.forEach((statusCode, location) -> errorHandler.addErrorPage(statusCode, location));
            handler.setErrorHandler(errorHandler);
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

        Collections.sort(sorted, Comparator.comparing(MappedFilter::getOrder));
        return sorted;
    }

    private List<MappedListener> sortedListeners(Set<MappedListener> unsorted) {
        List<MappedListener> sorted = new ArrayList<>(unsorted);

        Collections.sort(sorted, Comparator.comparing(MappedListener::getOrder));
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
     * @since 0.15
     */
    public String getContext() {
        return context;
    }

    @BQConfigProperty("Web application context path. Default is '/'.")
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

    @BQConfigProperty("A period in milliseconds specifying how long until an idle thread is terminated. ")
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

    @BQConfigProperty("Maximum number of requests to queue if the thread pool is busy. If this number is exceeded, " +
            "the server will start dropping requests.")
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

    @BQConfigProperty("Maximum number of request processing threads in the pool.")
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

    @BQConfigProperty("Minimal number of request processing threads in the pool.")
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
    @BQConfigProperty("A map of application-specific key/value parameters that are used as \"init\" parameters of the " +
            "ServletContext.")
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

    @BQConfigProperty("A boolean specifying whether servlet sessions should be supported by Jetty. The default is 'true'")
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
     * For security reasons this has to be set explicitly. There's no default.
     *
     * @param staticResourceBase A base location for resources of the Jetty context, that can
     *                           be a file path or a URL, as well as a special URL starting
     *                           with "classpath:".
     * @see JettyModuleExtender#useDefaultServlet()
     * @see JettyModuleExtender#addStaticServlet(String, String...)
     * @see <a href=
     * "http://download.eclipse.org/jetty/9.3.7.v20160115/apidocs/org/eclipse/jetty/servlet/DefaultServlet.html">
     * DefaultServlet</a>.
     * @since 0.13
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
     * @since 0.26
     */
    @BQConfigProperty("Replaces multiple '/'s with a single '/' in URL. Default value is 'false'.")
    public void setCompactPath(boolean compactPath) { this.compactPath = compactPath; }

    /**
     * Sets the maximum size of submitted forms in bytes. Default is 200000 (~195K).
     *
     * @param maxFormContentSize maximum size of submitted forms in bytes. Default is 200000 (~195K)
     * @since 0.22
     */
    @BQConfigProperty("Maximum size of submitted forms in bytes. Default is 200000 (~195K).")
    public void setMaxFormContentSize(int maxFormContentSize) {
        this.maxFormContentSize = maxFormContentSize;
    }

    /**
     * Sets the maximum number of form fields. Default is 1000.
     *
     * @param maxFormKeys maximum number of form fields. Default is 1000.
     * @since 0.22
     */
    @BQConfigProperty("Maximum number of form fields. Default is 1000.")
    public void setMaxFormKeys(int maxFormKeys) {
        this.maxFormKeys = maxFormKeys;
    }

    /**
     * @return a potentially null map of error pages configuration.
     * @since 0.24
     */
    public Map<Integer, String> getErrorPages() {
        return errorPages;
    }

    /**
     * Sets mappings between HTTP status codes and corresponding pages which will be returned to the user instead.
     *
     * @param errorPages map where keys are HTTP status codes and values are page URLs which will be used to handle them
     * @since 0.24
     */
    @BQConfigProperty("A map specifying a mapping between HTTP status codes and pages (URLs) which will be used as their handlers. If no mapping is specified then standard error handler is used.")
    public void setErrorPages(Map<Integer, String> errorPages) {
        this.errorPages = errorPages;
    }
}
