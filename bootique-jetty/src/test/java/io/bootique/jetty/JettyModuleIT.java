/**
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * “License”); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jetty;

import io.bootique.BQCoreModule;
import io.bootique.BQModule;
import io.bootique.BQRuntime;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class JettyModuleIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private BQRuntime startApp(BQModule module) {
        BQRuntime runtime = testFactory.app("-s")
                .module(module)
                .createRuntime();
        runtime.run();
        return runtime;
    }

    @Test
    public void addMappedServlet() {
        TestServlet s1 = new TestServlet();
        TestServlet s2 = new TestServlet();
        MappedServlet ms1 = new MappedServlet(s1, new HashSet<>(Arrays.asList("/a/*", "/b/*")));
        MappedServlet ms2 = new MappedServlet(s2, new HashSet<>(Arrays.asList("/c/*")));

        BQRuntime runtime = startApp(b -> JettyModule.extend(b).addMappedServlet(ms1).addMappedServlet(ms2));

        assertTrue(s1.wasInit);
        assertTrue(s2.wasInit);

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/a").request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), r1.getStatus());

        Response r2 = base.path("/b").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());

        Response r3 = base.path("/c").request().get();
        assertEquals(Status.OK.getStatusCode(), r3.getStatus());

        Response r4 = base.path("/d").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r4.getStatus());

        assertEquals(2, s1.serviceCounter.get());
        assertEquals(1, s2.serviceCounter.get());

        runtime.shutdown();
        assertTrue(s1.wasDestroyed);
        assertTrue(s2.wasDestroyed);
    }

    @Test
    public void addMappedFilter_InitDestroy() {

        TestFilter f1 = new TestFilter(1);
        TestFilter f2 = new TestFilter(2);

        MappedFilter mf1 = new MappedFilter(f1, Collections.singleton("/a/*"), 10);
        MappedFilter mf2 = new MappedFilter(f2, Collections.singleton("/a/*"), 0);

        BQRuntime runtime = startApp(b -> JettyModule.extend(b)
                .addMappedFilter(mf1)
                .addMappedFilter(mf2));

        Stream.of(f1, f2).forEach(f -> assertTrue(f.wasInit));

        runtime.shutdown();
        Stream.of(f1, f2).forEach(f -> assertTrue(f.wasDestroyed));
    }

    @Test
    public void addMappedFilter_Ordering() {

        TestFilter f1 = new TestFilter(1);
        TestFilter f2 = new TestFilter(2);
        TestFilter f3 = new TestFilter(3);

        MappedFilter mf1 = new MappedFilter(f1, Collections.singleton("/a/*"), 10);
        MappedFilter mf2 = new MappedFilter(f2, Collections.singleton("/a/*"), 0);
        MappedFilter mf3 = new MappedFilter(f3, Collections.singleton("/a/*"), 5);

        startApp(b -> JettyModule.extend(b)
                .addMappedFilter(mf1)
                .addMappedFilter(mf2)
                .addMappedFilter(mf3)
                // must have a servlet behind the filter chain...
                .addServlet(new TestServlet(), "last", "/a/*"));

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response response = base.path("/a").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        assertEquals("231", response.readEntity(String.class));
    }

    @Test
    public void addListener_ServletContextListener() {

        TestContextListener listener = new TestContextListener();

        BQRuntime runtime = startApp(b -> JettyModule.extend(b).addListener(listener));
        assertTrue(listener.wasInitialized);
        assertFalse(listener.wasDestroyed);

        runtime.shutdown();
        assertTrue(listener.wasDestroyed);
    }

    @Test
    public void addListener_ServletRequestListener() throws Exception {

        TestRequestListener listener = new TestRequestListener();

        startApp(b -> JettyModule.extend(b).addListener(listener));
        assertEquals(0, listener.initCounter.get());
        assertEquals(0, listener.destroyCounter.get());

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        base.path("/a").request().get();
        Thread.sleep(100);
        assertEquals(1, listener.initCounter.get());
        assertEquals(1, listener.destroyCounter.get());

        base.path("/b").request().get();
        Thread.sleep(100);
        assertEquals(2, listener.initCounter.get());
        assertEquals(2, listener.destroyCounter.get());

        // not_found request
        base.path("/c").request().get();
        Thread.sleep(100);
        assertEquals(3, listener.initCounter.get());
        assertEquals(3, listener.destroyCounter.get());
    }

    @Test
    public void addListener_SessionListener() throws Exception {

        // TODO: test session destroy event...

        HttpServlet s = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
                request.getSession(true);
            }
        };

        TestSessionListener listener = new TestSessionListener();

        startApp(b -> JettyModule.extend(b)
                .addServlet(s, "s1", "/a/*", "/b/*")
                .addListener(listener));

        assertEquals(0, listener.createCounter.get());
        assertEquals(0, listener.destroyCounter.get());

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        base.path("/a").request().get();
        Thread.sleep(100);
        assertEquals(1, listener.createCounter.get());
        assertEquals(0, listener.destroyCounter.get());

        base.path("/b").request().get();
        Thread.sleep(100);
        assertEquals(2, listener.createCounter.get());
        assertEquals(0, listener.destroyCounter.get());

        // not_found request
        base.path("/c").request().get();
        Thread.sleep(100);
        assertEquals(2, listener.createCounter.get());
        assertEquals(0, listener.destroyCounter.get());
    }

    @Test
    public void addListener_SessionListener_SessionsDisabled() throws Exception {

        HttpServlet s = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
                try {
                    request.getSession(true);
                } catch (IllegalStateException e) {
                    // expected, ignoring...
                }
            }
        };

        TestSessionListener listener = new TestSessionListener();

        startApp(b -> {
            JettyModule.extend(b)
                    .addServlet(s, "s1", "/a/*", "/b/*")
                    .addListener(listener);
            BQCoreModule.extend(b).setProperty("bq.jetty.sessions", "false");
        });

        assertEquals(0, listener.createCounter.get());
        assertEquals(0, listener.destroyCounter.get());

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        base.path("/a").request().get();
        Thread.sleep(100);
        assertEquals(0, listener.createCounter.get());
        assertEquals(0, listener.destroyCounter.get());
    }

    // TODO: tests for Attribute listeners

    static class TestServlet extends HttpServlet {

        boolean wasInit;
        boolean wasDestroyed;
        AtomicInteger serviceCounter = new AtomicInteger(0);

        @Override
        public void init(ServletConfig config) {
            wasInit = true;
        }

        @Override
        public void destroy() {
            this.wasDestroyed = true;
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) {
            serviceCounter.incrementAndGet();
        }
    }

    static class TestFilter implements Filter {
        final int id;
        boolean wasInit;
        boolean wasDestroyed;

        public TestFilter(int id) {
            this.id = id;
        }

        @Override
        public void init(FilterConfig filterConfig) {
            wasInit = true;
        }

        @Override
        public void destroy() {
            wasDestroyed = true;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            response.getWriter().append(String.valueOf(id));
            chain.doFilter(request, response);
        }
    }

    static class TestContextListener implements ServletContextListener {

        boolean wasInitialized;
        boolean wasDestroyed;

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            wasDestroyed = true;
        }

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            wasInitialized = true;
        }
    }

    static class TestRequestListener implements ServletRequestListener {

        AtomicInteger initCounter = new AtomicInteger(0);
        AtomicInteger destroyCounter = new AtomicInteger(0);

        @Override
        public void requestDestroyed(ServletRequestEvent sre) {
            destroyCounter.incrementAndGet();
        }

        @Override
        public void requestInitialized(ServletRequestEvent sre) {
            initCounter.incrementAndGet();
        }
    }

    static class TestSessionListener implements HttpSessionListener {
        AtomicInteger createCounter = new AtomicInteger(0);
        AtomicInteger destroyCounter = new AtomicInteger(0);

        @Override
        public void sessionCreated(HttpSessionEvent se) {
            createCounter.incrementAndGet();
        }

        @Override
        public void sessionDestroyed(HttpSessionEvent se) {
            destroyCounter.incrementAndGet();
        }
    }
}
