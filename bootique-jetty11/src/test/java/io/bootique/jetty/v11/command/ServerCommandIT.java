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

package io.bootique.jetty.v11.command;

import io.bootique.command.CommandOutcome;
import io.bootique.jetty.v11.JettyModule;
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
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class ServerCommandIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testRun() {

        CommandOutcome outcome = testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "x", "/"))
                .run();

        assertTrue(outcome.isSuccess());
        assertTrue(outcome.forkedToBackground());

        // testing that the server is in the operational state by the time ServerCommand exits...
        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("Hello World!", r.readEntity(String.class));
    }

    public static class TestServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setHeader("content-type", "text/plain");
            resp.getWriter().append("Hello World!");
        }
    }
}
