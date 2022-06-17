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

package io.bootique.jetty.jakarta;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demonstrates how one can define custom error handlers.
 */
@BQTest
public class ErrorPagesIT {

    static final String CUSTOM_404_RESPONSE = "custom 404 response";

    @BQApp
    public static BQRuntime app = Bootique.app("-s", "-c", "classpath:io/bootique/jetty/jakarta/error-pages.yml")
            .autoLoadModules()
            .module(b -> JettyModule.extend(b).addServlet(new CustomErrorHandler(), "error-handler", "/not-found-handler"))
            .createRuntime();

    @Test
    public void errorPagesHandlerCaptures404Request() {

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
