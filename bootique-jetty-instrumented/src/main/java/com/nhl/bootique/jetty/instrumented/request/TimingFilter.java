package com.nhl.bootique.jetty.instrumented.request;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.nhl.bootique.jetty.instrumented.InstrumentedRequestFilter;

/**
 * A servlet filter that provides metrics for request execution, and optionally
 * - logging of request start and finish marks.
 * 
 * @since 0.15
 */
public class TimingFilter implements Filter {

	/**
	 * Ordering used by the default configuration of this filter.
	 */
	public static final int DEFAULT_ORDER = -100;

	private static final Logger LOGGER = LoggerFactory.getLogger(TimingFilter.class);

	private Timer requestTimer;

	public TimingFilter(MetricRegistry metricRegistry) {
		super();
		this.requestTimer = metricRegistry.timer(MetricRegistry.name(InstrumentedRequestFilter.class, "request-timer"));
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// do nothing
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		Timer.Context requestTimerContext = requestTimer.time();

		try {

			// note that we are skipping request parameter/URL/etc. logging...
			// This is done by Jetty.

			LOGGER.info("request started");
			chain.doFilter(request, response);
		} finally {
			long timeNanos = requestTimerContext.stop();
			LOGGER.info("request finished in {} ms", timeNanos / 1000000);
		}
	}

}
