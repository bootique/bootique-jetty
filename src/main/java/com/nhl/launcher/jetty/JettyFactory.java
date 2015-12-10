package com.nhl.launcher.jetty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import com.nhl.launcher.jetty.server.HttpConnectorFactory;

public class JettyFactory {

	private String context;
	private int maxThreads;
	private int minThreads;
	private int maxQueuedRequests;
	private int idleThreadTimeout;
	private Collection<HttpConnectorFactory> connectors;

	public JettyFactory() {
		this.context = "/";
		this.minThreads = 4;
		this.maxThreads = 1024;
		this.maxQueuedRequests = 1024;
		this.idleThreadTimeout = 60000;
		
		this.connectors = new ArrayList<>();
		this.connectors.add(new HttpConnectorFactory());
	}

	public Server createServer() {
		ThreadPool threadPool = createThreadPool();
		Server server = new Server(threadPool);
		server.setStopAtShutdown(true);
		server.setHandler(createHandler());

		createConnectors(server, threadPool).forEach(c -> server.addConnector(c));

		// TODO: GZIP filter, request loggers, metrics, etc.

		return server;
	}

	protected Handler createHandler() {

		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath(context);
		return handler;
	}

	protected Stream<Connector> createConnectors(Server server, ThreadPool threadPool) {
		return connectors.stream().map(cf -> cf.createConnector(server, threadPool));
	}

	protected ThreadPool createThreadPool() {

		BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(minThreads, maxThreads, maxQueuedRequests);
		QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleThreadTimeout, queue);
		threadPool.setName("bootique-http");

		return threadPool;
	}

	public void setConnectors(Collection<HttpConnectorFactory> connectors) {
		this.connectors = connectors;
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
