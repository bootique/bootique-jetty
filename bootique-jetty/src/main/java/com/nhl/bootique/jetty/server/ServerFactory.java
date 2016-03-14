package com.nhl.bootique.jetty.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
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

	protected String context;
	protected int maxThreads;
	protected int minThreads;
	protected int maxQueuedRequests;
	protected int idleThreadTimeout;
	protected HttpConnectorFactory connector;
	protected Map<String, ServletFactory> servlets;
	protected Map<String, FilterFactory> filters;
	protected boolean sessions;

	public ServerFactory() {
		this.context = "/";
		this.minThreads = 4;
		this.maxThreads = 1024;
		this.maxQueuedRequests = 1024;
		this.idleThreadTimeout = 60000;
		this.sessions = true;

		this.connector = new HttpConnectorFactory();
	}

	public Server createServer(Set<MappedServlet> servlets, Set<MappedFilter> filters, Set<EventListener> listeners) {
		ThreadPool threadPool = createThreadPool();
		Server server = new Server(threadPool);
		server.setStopAtShutdown(true);
		server.setHandler(createHandler(servlets, filters, listeners));

		createConnectors(server, threadPool);

		// TODO: GZIP filter, request loggers, etc.

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
}
