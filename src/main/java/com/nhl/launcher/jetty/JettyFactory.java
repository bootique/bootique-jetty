package com.nhl.launcher.jetty;

import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

public class JettyFactory {

	private int port;
	private String context;
	private int maxThreads;
	private int minThreads;
	private int maxQueuedRequests;
	private int idleThreadTimeout;

	public JettyFactory() {
		this.port = 8080;
		this.context = "/";
		this.minThreads = 4;
		this.maxThreads = 1024;
		this.maxQueuedRequests = 1024;
		this.idleThreadTimeout = 60000;
	}

	public Server createServer() {
		ThreadPool tp = createThreadPool();
		Server server = new Server(tp);
		server.setStopAtShutdown(true);

		// TODO: GZIP filter, request loggers, metrics, etc.

		return server;
	}

	protected ThreadPool createThreadPool() {

		BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(minThreads, maxThreads, maxQueuedRequests);
		QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleThreadTimeout, queue);
		threadPool.setName("bootique-http");

		return threadPool;
	}

	public void setPort(int port) {
		this.port = port;
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
