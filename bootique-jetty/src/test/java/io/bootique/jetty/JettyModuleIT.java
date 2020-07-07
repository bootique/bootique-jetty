/**
 *  Licensed to ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.jetty;

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.di.BQModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionListener;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@BQTest
public class JettyModuleIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private Servlet mockServlet1;
    private Servlet mockServlet2;
    private Filter mockFilter1;
    private Filter mockFilter2;
    private Filter mockFilter3;

    @BeforeEach
    public void before() {

        this.mockServlet1 = mock(Servlet.class);
        this.mockServlet2 = mock(Servlet.class);

        this.mockFilter1 = mock(Filter.class);
        this.mockFilter2 = mock(Filter.class);
        this.mockFilter3 = mock(Filter.class);
    }

    private BQRuntime startApp(BQModule module) {
        BQRuntime runtime = testFactory.app("-s")
                .module(module)
                .createRuntime();
        runtime.run();
        return runtime;
    }

    @Test
    public void testContributeMappedServlets() throws Exception {

        MappedServlet mappedServlet1 = new MappedServlet(mockServlet1, new HashSet<>(Arrays.asList("/a/*", "/b/*")));
        MappedServlet mappedServlet2 = new MappedServlet(mockServlet2, new HashSet<>(Arrays.asList("/c/*")));

        BQRuntime runtime = startApp(b ->
                JettyModule.extend(b).addMappedServlet(mappedServlet1).addMappedServlet(mappedServlet2));

        verify(mockServlet1).init(any());
        verify(mockServlet2).init(any());

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/a").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());

        Response r2 = base.path("/b").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());

        Response r3 = base.path("/c").request().get();
        assertEquals(Status.OK.getStatusCode(), r3.getStatus());

        Response r4 = base.path("/d").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r4.getStatus());

        verify(mockServlet1, times(2)).service(any(), any());
        verify(mockServlet2).service(any(), any());

        runtime.shutdown();
        verify(mockServlet1).destroy();
        verify(mockServlet2).destroy();
    }

    @Test
    public void testContributeFilters_InitDestroy() {

        MappedFilter mf1 = new MappedFilter(mockFilter1, Collections.singleton("/a/*"), 10);
        MappedFilter mf2 = new MappedFilter(mockFilter2, Collections.singleton("/a/*"), 0);
        MappedFilter mf3 = new MappedFilter(mockFilter3, Collections.singleton("/a/*"), 5);

        BQRuntime runtime = startApp(b -> JettyModule.extend(b)
                .addMappedFilter(mf1)
                .addMappedFilter(mf2)
                .addMappedFilter(mf3));

        Arrays.asList(mockFilter1, mockFilter2, mockFilter3).forEach(f -> {
            try {
                verify(f).init(any());
            } catch (Exception e) {
                fail("init failed");
            }
        });

        runtime.shutdown();
        Arrays.asList(mockFilter1, mockFilter2, mockFilter3).forEach(f -> verify(f).destroy());
    }

    @Test
    public void testContributeFilters_Ordering() throws Exception {

        Filter[] mockFilters = new Filter[]{mockFilter1, mockFilter2, mockFilter3};

        for (int i = 0; i < mockFilters.length; i++) {

            String responseString = Integer.toString(i);
            doAnswer(inv -> {

                HttpServletRequest request = inv.getArgument(0);
                HttpServletResponse response = inv.getArgument(1);
                FilterChain chain = inv.getArgument(2);

                response.setStatus(200);
                response.getWriter().append(responseString);
                chain.doFilter(request, response);

                return null;
            }).when(mockFilters[i]).doFilter(any(), any(), any());
        }

        MappedFilter mf1 = new MappedFilter(mockFilter1, Collections.singleton("/a/*"), 10);
        MappedFilter mf2 = new MappedFilter(mockFilter2, Collections.singleton("/a/*"), 0);
        MappedFilter mf3 = new MappedFilter(mockFilter3, Collections.singleton("/a/*"), 5);

        startApp(b -> JettyModule.extend(b)
                .addMappedFilter(mf1)
                .addMappedFilter(mf2)
                .addMappedFilter(mf3)
                // must have a servlet behind the filter chain...
                .addServlet(mockServlet1, "last", "/a/*"));

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response response = base.path("/a").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        assertEquals("120", response.readEntity(String.class));
    }

    @Test
    public void testContributeListeners_ServletContextListener() {

        ServletContextListener scListener = mock(ServletContextListener.class);

        BQRuntime runtime = startApp(b -> JettyModule.extend(b).addListener(scListener));

        verify(scListener).contextInitialized(any());
        verify(scListener, times(0)).contextDestroyed(any());

        runtime.shutdown();
        verify(scListener).contextInitialized(any());
        verify(scListener).contextDestroyed(any());
    }

    @Test
    public void testContributeListeners_ServletRequestListener() throws Exception {

        ServletRequestListener srListener = mock(ServletRequestListener.class);

        startApp(b -> JettyModule.extend(b).addListener(srListener));

        verify(srListener, times(0)).requestInitialized(any());
        verify(srListener, times(0)).requestDestroyed(any());

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        base.path("/a").request().get();
        Thread.sleep(100);
        verify(srListener, times(1)).requestInitialized(any());
        verify(srListener, times(1)).requestDestroyed(any());

        base.path("/b").request().get();
        Thread.sleep(100);
        verify(srListener, times(2)).requestInitialized(any());
        verify(srListener, times(2)).requestDestroyed(any());

        // not_found request
        base.path("/c").request().get();
        Thread.sleep(100);
        verify(srListener, times(3)).requestInitialized(any());
        verify(srListener, times(3)).requestDestroyed(any());
    }

    @Test
    public void testContributeListeners_SessionListener() throws Exception {

        // TODO: test session destroy event...

        doAnswer(i -> {
            HttpServletRequest request = i.getArgument(0);
            request.getSession(true);
            return null;
        }).when(mockServlet1).service(any(ServletRequest.class), any(ServletResponse.class));

        HttpSessionListener sessionListener = mock(HttpSessionListener.class);

        startApp(b -> JettyModule.extend(b)
                .addServlet(mockServlet1, "s1", "/a/*", "/b/*")
                .addListener(sessionListener));

        verify(sessionListener, times(0)).sessionCreated(any());
        verify(sessionListener, times(0)).sessionDestroyed(any());

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        base.path("/a").request().get();
        Thread.sleep(100);
        verify(sessionListener, times(1)).sessionCreated(any());
        verify(sessionListener, times(0)).sessionDestroyed(any());

        base.path("/b").request().get();
        Thread.sleep(100);
        verify(sessionListener, times(2)).sessionCreated(any());
        verify(sessionListener, times(0)).sessionDestroyed(any());

        // not_found request
        base.path("/c").request().get();
        Thread.sleep(100);
        verify(sessionListener, times(2)).sessionCreated(any());
        verify(sessionListener, times(0)).sessionDestroyed(any());
    }

    @Test
    public void testContributeListeners_SessionListener_SessionsDisabled() throws Exception {

        doAnswer(i -> {
            HttpServletRequest request = (HttpServletRequest) i.getArgument(0);
            try {
                request.getSession(true);
            } catch (IllegalStateException e) {
                // expected, ignoring...
            }
            return null;
        }).when(mockServlet1).service(any(ServletRequest.class), any(ServletResponse.class));

        HttpSessionListener sessionListener = mock(HttpSessionListener.class);

        startApp(b -> {
            JettyModule.extend(b)
                    .addServlet(mockServlet1, "s1", "/a/*", "/b/*")
                    .addListener(sessionListener);
            BQCoreModule.extend(b).setProperty("bq.jetty.sessions", "false");
        });

        verify(sessionListener, times(0)).sessionCreated(any());
        verify(sessionListener, times(0)).sessionDestroyed(any());

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        base.path("/a").request().get();
        Thread.sleep(100);
        verify(sessionListener, times(0)).sessionCreated(any());
        verify(sessionListener, times(0)).sessionDestroyed(any());
    }

    // TODO: tests for Attribute listeners

}
