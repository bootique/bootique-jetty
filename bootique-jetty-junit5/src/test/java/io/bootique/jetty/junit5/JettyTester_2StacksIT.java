/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.jetty.junit5;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import java.io.IOException;

@BQTest
public class JettyTester_2StacksIT {

    static final JettyTester tester1 = JettyTester.create();

    @BQApp
    static final BQRuntime app1 = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> JettyModule.extend(b).addServlet(S1.class))
            .module(tester1.moduleReplacingConnectors())
            .createRuntime();

    static final JettyTester tester2 = JettyTester.create();

    @BQApp
    static final BQRuntime app2 = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> JettyModule.extend(b).addServlet(S2.class))
            .module(tester2.moduleReplacingConnectors())
            .createRuntime();

    @RepeatedTest(2)
    public void getTarget(RepetitionInfo repetitionInfo) {
        WebTarget t1 = tester1.getTarget();
        Response r1 = t1.request().get();
        JettyTester.assertOk(r1).assertContent("S1_" + repetitionInfo.getCurrentRepetition());

        WebTarget t2 = tester2.getTarget();
        Response r2 = t2.request().get();
        JettyTester.assertOk(r2).assertContent("S2_" + repetitionInfo.getCurrentRepetition());
    }

    @WebServlet(urlPatterns = "/*")
    static class S1 extends HttpServlet {

        static int times;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().append("S1_" + ++times);
        }
    }

    @WebServlet(urlPatterns = "/*")
    static class S2 extends HttpServlet {
        static int times;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().append("S2_" + ++times);
        }
    }
}
