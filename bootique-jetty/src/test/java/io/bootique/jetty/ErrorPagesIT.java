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

import io.bootique.test.junit5.BQTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demonstrates how one can define custom error handlers.
 */
public class ErrorPagesIT {

    static final String CUSTOM_404_RESPONSE = "custom 404 response";

    @RegisterExtension
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void errorPagesHandlerCaptures404Request() {
        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/error-pages.yml")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(new CustomErrorHandler(), "error-handler", "/not-found-handler"))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/this-page-does-not-exist").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r1.getStatus());
        assertEquals("custom 404 response", r1.readEntity(String.class));
    }

    static class CustomErrorHandler extends HttpServlet {
        private static final long serialVersionUID = -3190255883516320766L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            // Outputs custom message instead of the default 404.
            resp.getWriter().print(CUSTOM_404_RESPONSE);
        }
    }
}
