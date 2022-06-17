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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@BQTest
public class FilterInitParametersIT {

    @BQApp
    public static BQRuntime app = Bootique.app("-s", "-c", "classpath:io/bootique/jetty/FilterInitParametersIT.yml")
            .autoLoadModules()
            .module(b -> JettyModule.extend(b)
                    .addMappedFilter(new MappedFilter(new TestFilter(), Collections.singleton("/*"), "f1", 5))
                    .addMappedServlet(new MappedServlet(mock(Servlet.class), new HashSet<>(Arrays.asList("/*")))))
            .createRuntime();

    @Test
    public void testInitParametersPassed() {

        Map<String, String> params = new HashMap<>();
        params.put("a", "af1");
        params.put("b", "bf2");

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
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {

            HttpServletResponse resp = (HttpServletResponse) response;

            resp.setContentType("text/plain");

            resp.getWriter().print(config.getFilterName());
            resp.getWriter().print("_" + config.getInitParameter("a"));
            resp.getWriter().print("_" + config.getInitParameter("b"));
        }
    }

}
