package io.bootique.jetty.instrumented.server;

import com.codahale.metrics.MetricRegistry;
import io.bootique.jetty.MappedFilter;
import io.bootique.jetty.MappedServlet;
import io.bootique.jetty.instrumented.request.RequestTimer;
import io.bootique.jetty.server.ServerFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.EventListener;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

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
