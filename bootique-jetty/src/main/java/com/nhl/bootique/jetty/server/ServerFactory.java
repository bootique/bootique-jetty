package com.nhl.bootique.jetty.server;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhl.bootique.jetty.MappedFilter;
import com.nhl.bootique.jetty.MappedServlet;

public class ServerFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerFactory.class);

	private static final String CLASSPATH_URL_PREFIX = "classpath:";

	// handle custom URL protocols... TODO: this seems generally useful outside
	// of Jetty module
	protected static String preResolveResourceUrl(String resourceUrl) {

		if (resourceUrl == null) {
			return null;
		} else if (resourceUrl.startsWith(CLASSPATH_URL_PREFIX)) {
			URL url = ServerFactory.class.getClassLoader()
					.getResource(resourceUrl.substring(CLASSPATH_URL_PREFIX.length()));

			if (url == null) {
				LOGGER.warn("Ignorning unresolvable classpath url: " + resourceUrl);
				return resourceUrl;
			}

			return url.toString();
		} else {
			return resourceUrl;
		}
	}

	protected String context;
	protected int maxThreads;
	protected int minThreads;
	protected int maxQueuedRequests;
	protected int idleThreadTimeout;
	protected HttpConnectorFactory connector;
	protected Map<String, ServletFactory> servlets;
	protected Map<String, FilterFactory> filters;
	protected boolean sessions;
	private Map<String, String> params;
	private boolean staticResources;
	private String staticResourceBase;

	public ServerFactory() {
		this.staticResources = false;
		this.context = "/";
		this.minThreads = 4;
		this.maxThreads = 1024;
		this.maxQueuedRequests = 1024;
		this.idleThreadTimeout = 60000;
		this.sessions = true;

		this.connector = new HttpConnectorFactory();
	}

	public Server createServer(Set<MappedServlet> servlets, Set<MappedFilter> filters, Set<EventListener> listeners) {

		servlets = addDefaultServlets(servlets);

		ThreadPool threadPool = createThreadPool();
		Server server = new Server(threadPool);
		server.setStopAtShutdown(true);
		server.setHandler(createHandler(servlets, filters, listeners));

		createRequestLog(server);
		createConnectors(server, threadPool);

		// TODO: GZIP filter, request loggers, etc.

		return server;
	}

	protected Set<MappedServlet> addDefaultServlets(Set<MappedServlet> servlets) {
		if (staticResources) {
			servlets = new HashSet<>(servlets);
			servlets.add(createDefaultServlet());
		}

		return servlets;
	}

	protected MappedServlet createDefaultServlet() {

		DefaultServlet servlet = new DefaultServlet();

		return new MappedServlet(servlet, Collections.singleton("/"), "default");
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
			handler.setResourceBase(preResolveResourceUrl(staticResourceBase));
		}

		installListeners(handler, listeners);
		installServlets(handler, servlets);
		installFilters(handler, filters);

		return handler;
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

	protected void createConnectors(Server server, ThreadPool threadPool) {
		Connector c = connector.createConnector(server, threadPool);
		server.addConnector(c);
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

		Logger logger = LoggerFactory.getLogger(RequestLog.class);
		if (logger.isInfoEnabled()) {
			Slf4jRequestLog requestLog = new Slf4jRequestLog();
			requestLog.setExtended(true);
			server.setRequestLog(requestLog);
		}
	}

	public void setConnector(HttpConnectorFactory connector) {
		this.connector = connector;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public void setMaxThreads(int maxConnectorThreads) {
		this.maxThreads = maxConnectorThreads;
	}

	public void setMinThreads(int minThreads) {
		this.minThreads = minThreads;
	}

	public void setMaxQueuedRequests(int maxQueuedRequests) {
		this.maxQueuedRequests = maxQueuedRequests;
	}

	public void setIdleThreadTimeout(int idleThreadTimeout) {
		this.idleThreadTimeout = idleThreadTimeout;
	}

	public void setServlets(Map<String, ServletFactory> servlets) {
		this.servlets = servlets;
	}

	public void setFilters(Map<String, FilterFactory> filters) {
		this.filters = filters;
	}

	public void setSessions(boolean sessions) {
		this.sessions = sessions;
	}

	/**
	 * @since 0.13
	 * @param params
	 *            a map of context init parameters.
	 */
	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	/**
	 * @since 0.13
	 * @param staticResources
	 *            If true, Jetty will install Jetty DefaultServlet that would
	 *            serve static resources. The servlet is installed under the
	 *            name "default" and can be further configured via init
	 *            parameters passed from YAML. The value is "false" by default.
	 */
	public void setStaticResources(boolean staticResources) {
		this.staticResources = staticResources;
	}

	/**
	 * Sets a base location for resources of the Jetty context. The value can be
	 * a file path or a URL, as well as a special URL starting with
	 * "classpath:".
	 * <p>
	 * It can be optionally overridden by DefaultServlet configuration.
	 * <p>
	 * For security reasons this has to be set explicitly when "staticResources"
	 * is "true". There's no default.
	 * 
	 * @since 0.13
	 * @see <a href=
	 *      "http://download.eclipse.org/jetty/9.3.7.v20160115/apidocs/org/eclipse/jetty/servlet/DefaultServlet.html">
	 *      DefaultServlet</a>.
	 * @param staticResourceBase
	 *            A base location for resources of the Jetty context, that can
	 *            be a file path or a URL, as well as a special URL starting
	 *            with "classpath:".
	 */
	public void setStaticResourceBase(String staticResourceBase) {
		this.staticResourceBase = staticResourceBase;
	}
}
