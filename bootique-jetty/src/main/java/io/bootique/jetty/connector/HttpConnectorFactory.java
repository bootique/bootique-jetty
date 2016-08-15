package io.bootique.jetty.connector;

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
	private int responseHeaderSize;
	private int requestHeaderSize;

	public HttpConnectorFactory() {
		this.port = 8080;
		this.requestHeaderSize = 8 * 1024;
		this.responseHeaderSize = 8 * 1024;
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

		// most parameters are hardcoded for now... we should turn these
		// into properties

		httpConfig.setHeaderCacheSize(512);
		httpConfig.setOutputBufferSize(32 * 1024);
		httpConfig.setRequestHeaderSize(requestHeaderSize);
		httpConfig.setResponseHeaderSize(responseHeaderSize);
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

	/**
	 * @since 0.15
	 * @return configured listen port.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Sets a max size in bytes of Jetty request headers (and GET URLs). By
	 * default it is 8K.
	 * 
	 * @param requestHeaderSize
	 *            request header size value in bytes.
	 * @since 0.15
	 */
	public void setRequestHeaderSize(int requestHeaderSize) {
		this.requestHeaderSize = requestHeaderSize;
	}

	/**
	 * @since 0.15
	 * @return max size of Jetty request headers (and GET URLs).
	 */
	public int getRequestHeaderSize() {
		return requestHeaderSize;
	}

	/**
	 * Sets a max size in bytes of Jetty response headers. By default it is 8K.
	 * 
	 * @param responseHeaderSize
	 *            response header size value in bytes.
	 * @since 0.15
	 */
	public void setResponseHeaderSize(int responseHeaderSize) {
		this.responseHeaderSize = responseHeaderSize;
	}

	/**
	 * @since 0.15
	 * @return max size of Jetty response headers.
	 */
	public int getResponseHeaderSize() {
		return responseHeaderSize;
	}
}
