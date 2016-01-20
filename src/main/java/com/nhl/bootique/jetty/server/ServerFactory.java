package com.nhl.bootique.jetty.server;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhl.bootique.jetty.MappedFilter;
import com.nhl.bootique.jetty.MappedServlet;

public class ServerFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerFactory.class);

	private String context;
	private int maxThreads;
	private int minThreads;
	private int maxQueuedRequests;
	private int idleThreadTimeout;
	private HttpConnectorFactory connector;

	public ServerFactory() {
		this.context = "/";
		this.minThreads = 4;
		this.maxThreads = 1024;
		this.maxQueuedRequests = 1024;
		this.idleThreadTimeout = 60000;

		this.connector = new HttpConnectorFactory();
	}

	public Server createServer(Set<MappedServlet> servlets, Set<MappedFilter> filters) {
		ThreadPool threadPool = createThreadPool();
		Server server = new Server(threadPool);
		server.setStopAtShutdown(true);
		server.setHandler(createHandler(servlets, filters));

		createConnectors(server, threadPool);

		// TODO: GZIP filter, request loggers, metrics, etc.

		return server;
	}

	protected Handler createHandler(Set<MappedServlet> servlets, Set<MappedFilter> filters) {

		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath(context);

		servlets.forEach((servlet) -> {

			LOGGER.info("Adding servlet mapped to {}", servlet.getUrlPattern());
			handler.addServlet(new ServletHolder(servlet.getServlet()), servlet.getUrlPattern());
		});

		filters.forEach(filter -> {

			Objects.requireNonNull(filter.getFilter());

			if (filter.getUrlPatterns().isEmpty()) {
				LOGGER.info("Skipping unmapped filter {}", filter.getFilter().getClass().getName());
			} else {

				FilterHolder holder = new FilterHolder(filter.getFilter());
				EnumSet<DispatcherType> dispatches = EnumSet.of(DispatcherType.REQUEST);

				filter.getUrlPatterns().forEach(urlPattern -> {
					LOGGER.info("Adding filter mapped to {}", urlPattern);
					handler.addFilter(holder, urlPattern, dispatches);
				});
			}
		});

		return handler;
	}

	protected void createConnectors(Server server, ThreadPool threadPool) {
		Connector c = connector.createConnector(server, threadPool);
		server.addConnector(c);
	}

	protected ThreadPool createThreadPool() {

		BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(minThreads, maxThreads, maxQueuedRequests);
		QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleThreadTimeout, queue);
		threadPool.setName("bootique-http");

		return threadPool;
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
}
