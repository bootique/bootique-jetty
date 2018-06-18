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

import io.bootique.test.junit.BQTestFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class FilterInitParametersIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    private MappedServlet endOfChainServlet;

    @Before
    public void before() {
        Servlet mockServlet = mock(Servlet.class);
        endOfChainServlet = new MappedServlet(mockServlet, new HashSet<>(Arrays.asList("/*")));
    }

    @Test
    public void testInitParametersPassed() {

        Map<String, String> params = new HashMap<>();
        params.put("a", "af1");
        params.put("b", "bf2");

        MappedFilter mf = new MappedFilter(new TestFilter(), Collections.singleton("/*"), "f1", 5);

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/FilterInitParametersIT.yml")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addMappedFilter(mf).addMappedServlet(endOfChainServlet))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());

        assertEquals("f1_af1_bf2", r1.readEntity(String.class));
    }

    static class TestFilter implements Filter {

        private FilterConfig config;

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            this.config = filterConfig;
        }

        @Override
        public void destroy() {
            // do nothing...
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletResponse resp = (HttpServletResponse) response;

            resp.setContentType("text/plain");

            resp.getWriter().print(config.getFilterName());
            resp.getWriter().print("_" + config.getInitParameter("a"));
            resp.getWriter().print("_" + config.getInitParameter("b"));
        }
    }

}
