/**
 *    Licensed to the ObjectStyle LLC under one
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
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ContextInitParametersIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testInitParametersPassed() {

        Map<String, String> params = new HashMap<>();
        params.put("a", "a1");
        params.put("b", "b2");

        testFactory.app("-c", "classpath:io/bootique/jetty/ContextInitParametersIT.yml", "-s")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "s1", "/*"))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());

        assertEquals("s1_a1_b2", r1.readEntity(String.class));
    }

    static class TestServlet extends HttpServlet {
        private static final long serialVersionUID = -3190255883516320766L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");

            ServletConfig config = getServletConfig();

            resp.getWriter().print(config.getServletName());
            resp.getWriter().print("_" + config.getServletContext().getInitParameter("a"));
            resp.getWriter().print("_" + config.getServletContext().getInitParameter("b"));
        }
    }
}
