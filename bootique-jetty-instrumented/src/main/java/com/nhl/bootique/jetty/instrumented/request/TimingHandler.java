package com.nhl.bootique.jetty.instrumented.request;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * A servlet filter that provides metrics for request execution, and optionally
 * - logging of request start and finish marks.
 * 
 * @since 0.15
 */
public class TimingHandler implements Handler {

	private static final Logger LOGGER = LoggerFactory.getLogger(TimingHandler.class);

	private Timer requestTimer;
	private Handler delegate;

	public TimingHandler(MetricRegistry metricRegistry, Handler delegate) {
		this.requestTimer = metricRegistry.timer(MetricRegistry.name(TimingHandler.class, "request-timer"));
		this.delegate = delegate;
	}

	@Override
	public void start() throws Exception {
		delegate.start();
	}

	@Override
	public void stop() throws Exception {
		delegate.stop();
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		Timer.Context requestTimerContext = requestTimer.time();

		try {

			// note that we are skipping request parameter/URL/etc. logging...
			// This is done by Slf4jRequestLog. Here we only log timing

			LOGGER.info("request started");
			delegate.handle(target, baseRequest, request, response);
		} finally {
			long timeNanos = requestTimerContext.stop();
			LOGGER.info("request finished in {} ms", timeNanos / 1000000);
		}
	}

	@Override
	public boolean isRunning() {
		return delegate.isRunning();
	}

	@Override
	public boolean isStarted() {
		return delegate.isStarted();
	}

	@Override
	public boolean isStarting() {
		return delegate.isStarting();
	}

	@Override
	public boolean isStopping() {
		return delegate.isStopping();
	}

	@Override
	public boolean isStopped() {
		return delegate.isStopped();
	}

	@Override
	public boolean isFailed() {
		return delegate.isFailed();
	}

	@Override
	public void addLifeCycleListener(Listener listener) {
		delegate.addLifeCycleListener(listener);
	}

	@Override
	public void setServer(Server server) {
		delegate.setServer(server);
	}

	@Override
	public void removeLifeCycleListener(Listener listener) {
		delegate.removeLifeCycleListener(listener);
	}

	@Override
	public Server getServer() {
		return delegate.getServer();
	}

	@Override
	public void destroy() {
		delegate.destroy();
	}

}
