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
package io.bootique.jetty.docs;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import java.io.IOException;


// tag::declarations[]
@BQTest
public class JettyIT {

    static final JettyTester jetty = JettyTester.create(); // <1>

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(jetty.moduleReplacingConnectors()) // <2>
            // end::declarations[]
            .module(b -> JettyModule.extend(b).addServlet(HWServlet.class))
            // tag::declarations[]
            .createRuntime();
    // end::declarations[]

    // tag::test[]
    @Test
    public void test() {
        Response ok = jetty.getTarget() // <1>
                .path("helloworld").request().get();

        JettyTester.assertOk(ok).assertContent("Hello, world!"); // <2>
    }
    // end::test[]

    @WebServlet(urlPatterns = "/helloworld")
    static class HWServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().print("Hello, world!");
        }
    }
}

