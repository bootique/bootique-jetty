package com.nhl.bootique.jetty.server;

import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;

public class HttpConnectorFactory {

	private int port;

	public HttpConnectorFactory() {
		this.port = 8080;
	}

	public Connector createConnector(Server server, ThreadPool threadPool) {

		// a few things are hardcoded for now... if needed we can turn these
		// into properties

		HttpConfiguration httpConfig = buildHttpConfiguration();
		ConnectionFactory connectionFactory = buildHttpConnectionFactory(httpConfig);
		Scheduler scheduler = new ScheduledExecutorScheduler();
		ByteBufferPool bufferPool = buildBufferPool();
		int selectorThreads = Runtime.getRuntime().availableProcessors();
		int acceptorThreads = Math.max(1, selectorThreads / 2);

		ServerConnector connector = new ServerConnector(server, threadPool, scheduler, bufferPool, acceptorThreads,
				selectorThreads, connectionFactory);
		connector.setPort(port);
		connector.setIdleTimeout(30 * 1000);

		return connector;
	}

	protected HttpConnectionFactory buildHttpConnectionFactory(HttpConfiguration httpConfig) {
		return new HttpConnectionFactory(httpConfig);
	}

	protected HttpConfiguration buildHttpConfiguration() {

		HttpConfiguration httpConfig = new HttpConfiguration();

		// hardcoded for now... if needed we can turn these into properties

		httpConfig.setHeaderCacheSize(512);
		httpConfig.setOutputBufferSize(32 * 1024);
		httpConfig.setRequestHeaderSize(8 * 1024);
		httpConfig.setResponseHeaderSize(8 * 1024);
		httpConfig.setSendDateHeader(true);
		httpConfig.setSendServerVersion(true);

		httpConfig.addCustomizer(new ForwardedRequestCustomizer());

		return httpConfig;
	}

	protected ByteBufferPool buildBufferPool() {
		// hardcoded for now... if needed we can turn these into properties
		return new ArrayByteBufferPool(64, 1024, 64 * 1024);
	}

	public void setPort(int port) {
		this.port = port;
	}
}
