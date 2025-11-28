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

import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class MappedFilterIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void mappedConfig() {
        TestFilter filter = new TestFilter();

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addFilter(filter, "f1", 0, "/a/*", "/b/*"))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/a").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());

        Response r2 = base.path("/b").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());

        Response r3 = base.path("/c").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r3.getStatus());

        assertEquals(2, filter.filterCounter.get());
    }

    @Test
    public void mappedConfig_Override() {
        TestFilter filter = new TestFilter();

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/MappedFilterIT1.yml")
                .module(b -> JettyModule.extend(b).addFilter(filter, "f1", 0, "/a/*", "/b/*"))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/a").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r1.getStatus());

        Response r2 = base.path("/b").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r2.getStatus());

        Response r3 = base.path("/c").request().get();
        assertEquals(Status.OK.getStatusCode(), r3.getStatus());

        assertEquals(1, filter.filterCounter.get());
    }

    static class TestFilter implements Filter {

        AtomicInteger filterCounter = new AtomicInteger();

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            filterCounter.incrementAndGet();
        }
    }

}
