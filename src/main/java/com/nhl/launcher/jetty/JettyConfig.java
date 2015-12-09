package com.nhl.launcher.jetty;

import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

public class JettyConfig {

	private int port;
	private String context;
	private int maxThreads;
	private int minThreads;
	private int maxQueuedRequests;
	private int idleThreadTimeout;

	public JettyConfig() {
		this.port = 8080;
		this.context = "/";
		this.minThreads = 4;
		this.maxThreads = 1024;
		this.maxQueuedRequests = 1024;
		this.idleThreadTimeout = 60000;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxConnectorThreads) {
		this.maxThreads = maxConnectorThreads;
	}

	public int getMinThreads() {
		return minThreads;
	}

	public void setMinThreads(int minThreads) {
		this.minThreads = minThreads;
	}

	public Server createServer() {
		ThreadPool tp = createThreadPool();
		Server server = new Server(tp);

		// TODO: GZIP filter, request loggers, metrics, etc.
		
		return server;
	}

	protected ThreadPool createThreadPool() {

		BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(minThreads, maxThreads, maxQueuedRequests);
		QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleThreadTimeout, queue);
		threadPool.setName("bq");

		return threadPool;
	}
}
