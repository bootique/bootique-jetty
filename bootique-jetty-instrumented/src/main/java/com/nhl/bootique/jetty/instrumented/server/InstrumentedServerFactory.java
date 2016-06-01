package com.nhl.bootique.jetty.instrumented.server;

import java.util.EventListener;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.codahale.metrics.MetricRegistry;
import com.nhl.bootique.jetty.MappedFilter;
import com.nhl.bootique.jetty.MappedServlet;
import com.nhl.bootique.jetty.instrumented.request.RequestTimer;
import com.nhl.bootique.jetty.server.ServerFactory;

/**
 * @since 0.11
 */
public class InstrumentedServerFactory extends ServerFactory {

	private MetricRegistry metricRegistry;

	@Override
	protected QueuedThreadPool createThreadPool(BlockingQueue<Runnable> queue) {
		return new InstrumentedQueuedThreadPool(maxThreads, minThreads, idleThreadTimeout, metricRegistry);
	}

	public InstrumentedServerFactory initMetricRegistry(MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;
		return this;
	}
	
	@Override
	protected Handler createHandler(Set<MappedServlet> servlets, Set<MappedFilter> filters,
			Set<EventListener> listeners) {

		Handler delegate =  super.createHandler(servlets, filters, listeners);
		return new RequestTimer(metricRegistry, delegate);
	}
}
