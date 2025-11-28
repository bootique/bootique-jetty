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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class MappedServletIT {

    private static final WebTarget target = ClientBuilder.newClient().target("http://localhost:8080");

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();


    @Test
    public void mappedConfig() {

        TestServlet servlet = new TestServlet();
        MappedServlet<TestServlet> mappedServlet = new MappedServlet<>(servlet, Set.of("/a/*", "/b/*"));

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addMappedServlet(mappedServlet))
                .run();

        Response r1 = target.path("/a").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());

        Response r2 = target.path("/b").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());

        Response r3 = target.path("/c").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r3.getStatus());

        assertEquals(2, servlet.serviceCounter.get());
    }

    @Test
    public void mappedConfig_Override() throws Exception {
        TestServlet servlet = new TestServlet();
        MappedServlet<TestServlet> mappedServlet = new MappedServlet<>(servlet, Set.of("/a/*", "/b/*"), "s1");

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/MappedServletIT1.yml")
                .module(b -> JettyModule.extend(b).addMappedServlet(mappedServlet))
                .run();

        Response r1 = target.path("/a").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r1.getStatus());

        Response r2 = target.path("/b").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r2.getStatus());

        Response r3 = target.path("/c").request().get();
        assertEquals(Status.OK.getStatusCode(), r3.getStatus());

        assertEquals(1, servlet.serviceCounter.get());
    }

    static class TestServlet extends HttpServlet {
        AtomicInteger serviceCounter = new AtomicInteger();

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            serviceCounter.incrementAndGet();
        }
    }

}
