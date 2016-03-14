package com.nhl.bootique.jetty.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.List;
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

	protected String context;
	protected int maxThreads;
	protected int minThreads;
	protected int maxQueuedRequests;
	protected int idleThreadTimeout;
	protected HttpConnectorFactory connector;

	public ServerFactory() {
		this.context = "/";
		this.minThreads = 4;
		this.maxThreads = 1024;
		this.maxQueuedRequests = 1024;
		this.idleThreadTimeout = 60000;

		this.connector = new HttpConnectorFactory();
	}

	public Server createServer(Set<MappedServlet> servlets, Set<MappedFilter> filters, Set<EventListener> listeners) {
		ThreadPool threadPool = createThreadPool();
		Server server = new Server(threadPool);
		server.setStopAtShutdown(true);
		server.setHandler(createHandler(servlets, filters, listeners));

		createConnectors(server, threadPool);

		// TODO: GZIP filter, request loggers, metrics, etc.

		return server;
	}

	protected Handler createHandler(Set<MappedServlet> servlets, Set<MappedFilter> filters,
			Set<EventListener> listeners) {

		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath(context);

		listeners.forEach(listener -> {

			LOGGER.info("Adding listener {}", listener.getClass().getName());
			handler.addEventListener(listener);
		});

		servlets.forEach((servlet) -> {

			Objects.requireNonNull(servlet.getServlet());

			if (servlet.getUrlPatterns().isEmpty()) {
				LOGGER.info("Skipping unmapped servlet {}", servlet.getServlet().getClass().getName());
			} else {

				ServletHolder holder = new ServletHolder(servlet.getServlet());
				
				if(servlet.getName() != null) {
					holder.setName(servlet.getName());
				}
				
				servlet.getInitParams().forEach((k, v) -> holder.setInitParameter(k, v));

				servlet.getUrlPatterns().forEach(urlPattern -> {
					LOGGER.info("Adding servlet mapped to {}", urlPattern);
					handler.addServlet(holder, urlPattern);
				});
			}

		});

		sortedFilters(filters).forEach(filter -> {

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
}
