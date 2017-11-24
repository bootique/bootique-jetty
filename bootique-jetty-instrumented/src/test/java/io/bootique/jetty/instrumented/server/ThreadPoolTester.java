package io.bootique.jetty.instrumented.server;

import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.instrumented.unit.InstrumentedJettyApp;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

/**
 * Helps to build an assembly with a testable thread pool state. Specifically allows to freeze some request threads in
 * the middle of a request, so that the tests can check reported pool statistics.
 */
class ThreadPoolTester {

    private InstrumentedJettyApp app;
    private int parallelRequests;
    private Consumer<BQRuntime> checkAfterStartup;
    private Consumer<BQRuntime> checkWithRequestsFrozen;

    public ThreadPoolTester(InstrumentedJettyApp app) {
        this.app = app;
        this.parallelRequests = 2;
    }

    public ThreadPoolTester parallelRequests(int count) {
        this.parallelRequests = count;
        return this;
    }

    public ThreadPoolTester checkAfterStartup(Consumer<BQRuntime> task) {
        this.checkAfterStartup = task;
        return this;
    }

    public ThreadPoolTester checkWithRequestsFrozen(Consumer<BQRuntime> task) {
        this.checkWithRequestsFrozen = task;
        return this;
    }

    @Test
    public void test(String config) throws InterruptedException {

        ExecutorService clientPool = Executors.newFixedThreadPool(parallelRequests);

        Lock requestFreezeLock = new ReentrantLock();
        CountDownLatch releaseOnRequestQueuedUpLatch = new CountDownLatch(parallelRequests);
        CountDownLatch releaseAfterRequestLatch = new CountDownLatch(parallelRequests);

        BQRuntime runtime = app.start(
                b -> JettyModule.extend(b).addServlet(new TestServlet(requestFreezeLock,
                        releaseOnRequestQueuedUpLatch,
                        releaseAfterRequestLatch), "s1", "/*"),
                "-c",
                config);

        if (checkAfterStartup != null) {
            checkAfterStartup.accept(runtime);
        }

        // start requests
        requestFreezeLock.lock();
        WebTarget target = ClientBuilder.newClient().target("http://localhost:8080").path("/");
        try {
            for (int i = 0; i < parallelRequests; i++) {
                clientPool.submit(() -> target.request().get());
            }

            assertTrue("Requests failed to queue up in 1 sec", releaseOnRequestQueuedUpLatch.await(1, TimeUnit.SECONDS));

            if (checkWithRequestsFrozen != null) {
                checkWithRequestsFrozen.accept(runtime);
            }

        } finally {
            requestFreezeLock.unlock();
            clientPool.shutdownNow();
            assertTrue("Queued requests failed to clear in 1 sec", releaseAfterRequestLatch.await(1, TimeUnit.SECONDS));
        }
    }

    static class TestServlet extends HttpServlet {

        private Lock requestFreezeLock;
        private CountDownLatch releaseOnRequestFinish;
        private CountDownLatch releaseOnRequestQueuedUp;

        public TestServlet(Lock requestFreezeLock, CountDownLatch releaseOnRequestQueuedUp, CountDownLatch releaseOnRequestFinish) {
            this.requestFreezeLock = requestFreezeLock;
            this.releaseOnRequestQueuedUp = releaseOnRequestQueuedUp;
            this.releaseOnRequestFinish = releaseOnRequestFinish;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

            // handle locks...

            // 1. let the container know  that the client request has arrived
            releaseOnRequestQueuedUp.countDown();

            // 2. but don't proceed until allowed by the caller, effectively freezing the state so that we can count thread stats
            requestFreezeLock.lock();

            try {
                sendResponse(response);
            } finally {

                // 3. let the container know that we are done
                releaseOnRequestFinish.countDown();

                // 4. since we locked it, we must unlock
                requestFreezeLock.unlock();
            }
        }

        private void sendResponse(HttpServletResponse response) throws IOException {
            response.setContentType("text/plain");
            response.getWriter().print("Hi!");
        }
    }
}
