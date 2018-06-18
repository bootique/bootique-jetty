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

package io.bootique.jetty.servlet;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.bootique.jetty.JettyModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AnnotatedFilterIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();
    private Runnable assertion;

    @After
    public void after() {
        assertion = null;
    }

    @Test
    public void testServletContainerState() {
        testFactory.app("-s")
                .module(new FilterModule())
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        assertNull(assertion);

        base.path("/b").request().get().close();
        assertNotNull(assertion);
        assertion = null;

        base.path("/b/1").request().get().close();
        assertNotNull(assertion);
        assertion = null;

        base.path("/b/2").request().get().close();
        assertNotNull(assertion);
    }

    @Test
    public void testConfig_Override() {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/servlet/AnnotatedFilterIT1.yml")
                .module(new FilterModule())
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        assertNull(assertion);

        base.path("/b").request().get();
        assertNull(assertion);

        base.path("/c").request().get();
        Objects.requireNonNull(assertion).run();
        assertion = null;
    }

    class FilterModule implements Module {

        @Override
        public void configure(Binder binder) {
            JettyModule.extend(binder).addFilter(AnnotatedFilter.class);
        }

        @Provides
        private AnnotatedFilter provideFilter() {
            return new AnnotatedFilter();
        }

        @WebFilter(filterName = "f1", urlPatterns = "/b/*")
        class AnnotatedFilter implements Filter {

            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
                // do nothing
            }

            @Override
            public void destroy() {
                // do nothing
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                assertion = () -> {
                };
            }
        }
    }

}
