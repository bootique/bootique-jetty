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

package io.bootique.jetty.v11;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class ContextInitParametersIT {

    @BQApp
    public static BQRuntime app = Bootique
            .app("-c", "classpath:io/bootique/jetty/v11/ContextInitParametersIT.yml", "-s")
            .autoLoadModules()
            .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "s1", "/*"))
            .createRuntime();

    @Test
    public void testInitParametersPassed() {

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
