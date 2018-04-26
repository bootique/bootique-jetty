package io.bootique.jetty.instrumented.unit;

import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.test.junit.BQTestFactory;

import javax.servlet.Servlet;
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
public class ThreadPoolTester {

    private BQTestFactory app;
    private int unblockAfterInProgressRequests;
    private int sendRequests;
    private Consumer<BQRuntime> afterStartup;
    private Consumer<BQRuntime> afterRequestsFrozen;

    public ThreadPoolTester(BQTestFactory app) {
        this.app = app;
        this.unblockAfterInProgressRequests = 2;
        this.sendRequests = 2;
    }

    public ThreadPoolTester unblockAfterInProgressRequests(int count) {
        this.unblockAfterInProgressRequests = count;
        return this;
    }

    public ThreadPoolTester sendRequests(int count) {
        this.sendRequests = count;
        return this;
    }

    public ThreadPoolTester afterStartup(Consumer<BQRuntime> task) {
        this.afterStartup = task;
        return this;
    }

    public ThreadPoolTester afterRequestsFrozen(Consumer<BQRuntime> task) {
        this.afterRequestsFrozen = task;
        return this;
    }

    public void run(String config) throws InterruptedException {

        Locks locks = new Locks(sendRequests, unblockAfterInProgressRequests);

        BQRuntime runtime = startRuntime(config, new FreezePoolServlet(locks));
        runChecksAfterStartup(runtime);
        new RequestRunner(runtime, locks).run();
    }

    private BQRuntime startRuntime(String config, Servlet servlet) {
        BQRuntime runtime = app.app("-s", "-c", config)
                .module(
                b -> JettyModule.extend(b).addServlet(servlet, "s1", "/*"))
                .createRuntime();

        runtime.run();
        return runtime;
    }

    private void runChecksAfterStartup(BQRuntime runtime) {
        if (afterStartup != null) {
            afterStartup.accept(runtime);
        }
    }

    private void runChecksWithRequestsFrozen(BQRuntime runtime) {
        if (afterRequestsFrozen != null) {
            afterRequestsFrozen.accept(runtime);
        }
    }

    static class Locks {

        Lock requestFreezeLock;
        CountDownLatch releaseOnRequestQueuedUp;
        CountDownLatch releaseAfterRequestLatch;

        public Locks(int startRequests, int unblockAfter) {
            requestFreezeLock = new ReentrantLock();
            releaseOnRequestQueuedUp = new CountDownLatch(unblockAfter);
            releaseAfterRequestLatch = new CountDownLatch(startRequests);
        }
    }

    static class FreezePoolServlet extends HttpServlet {

        private Locks locks;

        public FreezePoolServlet(Locks locks) {
            this.locks = locks;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

            // handle locks...

            // 1. let the container know  that the client request has arrived
            locks.releaseOnRequestQueuedUp.countDown();

            // 2. but don't proceed until allowed by the caller, effectively freezing the state so that we can count thread stats
            locks.requestFreezeLock.lock();

            try {
                sendResponse(response);
            } finally {

                // 3. since we locked it, we must unlock
                locks.requestFreezeLock.unlock();

                // 4. let the container know that we are done
                locks.releaseAfterRequestLatch.countDown();
            }
        }

        private void sendResponse(HttpServletResponse response) throws IOException {
            response.setContentType("text/plain");
            response.getWriter().print("Hi!");
        }
    }

    class RequestRunner {

        private BQRuntime runtime;
        private Locks locks;

        public RequestRunner(BQRuntime runtime, Locks locks) {
            this.runtime = runtime;
            this.locks = locks;
        }

        public void run() throws InterruptedException {

            ExecutorService clientPool = Executors.newCachedThreadPool();

            try {
                runWithClientPool(clientPool);
            } finally {
                clientPool.shutdownNow();
            }
        }

        private void runWithClientPool(ExecutorService clientPool) throws InterruptedException {

            locks.requestFreezeLock.lock();
            WebTarget target = ClientBuilder.newClient().target("http://localhost:8080");

            try {
                for (int i = 0; i < sendRequests; i++) {
                    clientPool.submit(() -> target.request().get().close());
                }

                assertTrue("Requests failed to queue up in 1 sec", locks.releaseOnRequestQueuedUp.await(1, TimeUnit.SECONDS));

                runChecksWithRequestsFrozen(runtime);

            } finally {
                locks.requestFreezeLock.unlock();
                assertTrue("Queued requests failed to clear in 1 sec", locks.releaseAfterRequestLatch.await(1, TimeUnit.SECONDS));
            }
        }
    }
}
