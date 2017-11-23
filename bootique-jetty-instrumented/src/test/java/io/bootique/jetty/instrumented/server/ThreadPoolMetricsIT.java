package io.bootique.jetty.instrumented.server;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.instrumented.unit.InstrumentedJettyApp;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ThreadPoolMetricsIT {

    @Rule
    public InstrumentedJettyApp app = new InstrumentedJettyApp();

    private WebTarget client;
    private ExecutorService clientPool;
    private Lock serverLock;

    @Before
    public void before() {
        clientPool = Executors.newCachedThreadPool();
        client = ClientBuilder.newClient().target("http://localhost:8080");
        serverLock = new ReentrantLock();
    }

    @After
    public void after() throws InterruptedException {
        serverLock.unlock();
        clientPool.shutdownNow();

        // allowing hung servlets to finish after the unlock above. Otherwise following
        // tests may get incorrect thread state
        Thread.sleep(200);
    }

    private Gauge<Double> findUtilizationVsMaxGauge(BQRuntime runtime) {
        return findGauge(Double.class, runtime, "utilization-max");
    }

    private <T> Gauge<T> findGauge(Class<T> type, BQRuntime runtime, String label) {

        MetricRegistry registry = runtime.getInstance(MetricRegistry.class);
        String name = MetricRegistry.name(QueuedThreadPool.class, "bootique-http", label);

        Collection<Gauge> gauges = registry.getGauges((n, m) -> name.equals(n)).values();
        assertEquals("Unexpected number of gauges for " + name, 1, gauges.size());
        return gauges.iterator().next();
    }

    @Test
    public void testUtilizationVsMax() throws InterruptedException {

        serverLock.lock();
        CountDownLatch clientLatch = new CountDownLatch(2);

        BQRuntime runtime = app.start(
                b -> JettyModule.extend(b).addServlet(new TestServlet(serverLock, clientLatch), "s1", "/*"),
                "-c",
                "classpath:threads.yml");

        Gauge<Double> utilizationVsMax = findUtilizationVsMaxGauge(runtime);

        // utilizationMax = (acceptorTh + selectorTh + active) / max
        // see more detailed explanation in InstrumentedQueuedThreadPool
        assertEquals((3 + 4 + 0) / 30d, utilizationVsMax.getValue(), 0.0001);

        for (int i = 0; i < 2; i++) {
            clientPool.submit(() -> client.path("/").request().get());
        }

        assertTrue("Requests failed to queue up in 1 sec", clientLatch.await(1, TimeUnit.SECONDS));

        // utilizationMax = (acceptorTh + selectorTh + active) / max
        // see more detailed explanation in InstrumentedQueuedThreadPool
        assertEquals((3 + 4 + 2) / 30d , utilizationVsMax.getValue(), 0.0001);
    }

    static class TestServlet extends HttpServlet {

        private Lock serverLock;
        private CountDownLatch clientLock;

        public TestServlet(Lock serverLock, CountDownLatch clientLock) {
            this.serverLock = serverLock;
            this.clientLock = clientLock;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            // handle locks...

            // 1. tell the caller that the client request has arrived
            clientLock.countDown();

            // 2. but keep the request processor locked so that we can count threads in the test
            serverLock.lock();

            try {
                resp.setContentType("text/plain");
                resp.getWriter().print("test_servlet");
            } finally {
                serverLock.unlock();
            }
        }
    }
}
