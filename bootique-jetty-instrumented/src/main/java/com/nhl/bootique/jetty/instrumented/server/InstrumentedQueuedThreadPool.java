package com.nhl.bootique.jetty.instrumented.server;

import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;

// very similar to Dropwizard InstrumentedQueuedThreadPool at
// https://github.com/dropwizard/metrics/blob/master/metrics-jetty9/src/main/java/io/dropwizard/metrics/jetty9/InstrumentedQueuedThreadPool.java
public class InstrumentedQueuedThreadPool extends QueuedThreadPool {

	private MetricRegistry metricRegistry;

	public InstrumentedQueuedThreadPool(MetricRegistry metricRegistry) {
		super();
		this.metricRegistry = metricRegistry;
	}

	public InstrumentedQueuedThreadPool(int maxThreads, int minThreads, int idleTimeout, BlockingQueue<Runnable> queue,
			ThreadGroup threadGroup, MetricRegistry metricRegistry) {
		super(maxThreads, minThreads, idleTimeout, queue, threadGroup);
		this.metricRegistry = metricRegistry;
	}

	public InstrumentedQueuedThreadPool(int maxThreads, int minThreads, int idleTimeout, BlockingQueue<Runnable> queue,
			MetricRegistry metricRegistry) {
		super(maxThreads, minThreads, idleTimeout, queue);
		this.metricRegistry = metricRegistry;
	}

	public InstrumentedQueuedThreadPool(int maxThreads, int minThreads, int idleTimeout,
			MetricRegistry metricRegistry) {
		super(maxThreads, minThreads, idleTimeout);
		this.metricRegistry = metricRegistry;
	}

	public InstrumentedQueuedThreadPool(int maxThreads, int minThreads, MetricRegistry metricRegistry) {
		super(maxThreads, minThreads);
		this.metricRegistry = metricRegistry;
	}

	public InstrumentedQueuedThreadPool(int maxThreads, MetricRegistry metricRegistry) {
		super(maxThreads);
		this.metricRegistry = metricRegistry;
	}

	@Override
	protected void doStart() throws Exception {

		super.doStart();

		metricRegistry.register(MetricRegistry.name(QueuedThreadPool.class, getName(), "utilization"),
				new RatioGauge() {
					@Override
					protected Ratio getRatio() {
						int threads = getThreads();
						return Ratio.of(threads - getIdleThreads(), threads);
					}
				});
		
		metricRegistry.register(MetricRegistry.name(QueuedThreadPool.class, getName(), "utilization-max"),
				new RatioGauge() {
					@Override
					protected Ratio getRatio() {
						return Ratio.of(getThreads() - getIdleThreads(), getMaxThreads());
					}
				});
		
		metricRegistry.register(MetricRegistry.name(QueuedThreadPool.class, getName(), "size"), new Gauge<Integer>() {
			
			@Override
			public Integer getValue() {
				return getThreads();
			}
		});
		
		metricRegistry.register(MetricRegistry.name(QueuedThreadPool.class, getName(), "queued-requests"), new Gauge<Integer>() {
			
			@Override
			public Integer getValue() {
				// This assumes the QueuedThreadPool is using a
				// BlockingArrayQueue or
				// ArrayBlockingQueue for its queue, and is therefore a
				// constant-time operation.
				return getQueue().size();
			}
		});
	}

}
