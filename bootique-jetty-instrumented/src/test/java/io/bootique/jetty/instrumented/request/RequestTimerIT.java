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

package io.bootique.jetty.instrumented.request;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.bootique.BQRuntime;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class RequestTimerIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    final JettyTester tester = JettyTester.create();

    @Test
    public void initParametersPassed() {

        BQRuntime runtime = testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "s1", "/*"))
                .module(tester.moduleReplacingConnectors())
                .createRuntime();

        runtime.run();

        WebTarget target = tester.getTarget();

        Response r1 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());

        assertEquals("test_servlet", r1.readEntity(String.class));

        MetricRegistry metrics = runtime.getInstance(MetricRegistry.class);

        Collection<Timer> timers = metrics.getTimers().values();
        assertEquals(1, timers.size());

        Timer timer = timers.iterator().next();
        assertEquals(1, timer.getCount());

        target.request().get().close();
        assertEquals(2, timer.getCount());
    }

    static class TestServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            resp.getWriter().print("test_servlet");
        }
    }

}
