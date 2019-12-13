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

package io.bootique.jetty.server;

import io.bootique.BQRuntime;
import io.bootique.di.Binder;
import io.bootique.di.BQModule;
import io.bootique.jetty.JettyModule;
import io.bootique.test.junit.BQTestFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ServerFactoryConnectorsIT {

    private static final String OUT_CONTENT = "xcontent_stream_content_stream";

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private Client client = ClientBuilder.newClient();

    @Test
    public void testMultipleConnectors() {

        BQRuntime runtime = testFactory.app("-s", "-c", "classpath:io/bootique/jetty/server/connectors.yml")
                .module(new UnitModule())
                .createRuntime();

        runtime.run();

        // deprecated default connector must NOT be started
        Connector[] connectors = runtime.getInstance(Server.class).getConnectors();
        assertEquals(2, connectors.length);

        Response r1NormalConnector = client.target("http://localhost:14001/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1NormalConnector.getStatus());
        assertEquals(OUT_CONTENT, r1NormalConnector.readEntity(String.class));

        Response r2NormalConnector = client.target("http://localhost:14002/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2NormalConnector.getStatus());
        assertEquals(OUT_CONTENT, r2NormalConnector.readEntity(String.class));
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        private static final long serialVersionUID = -8896839263652092254L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.getWriter().append(OUT_CONTENT);
        }
    }

    class UnitModule implements BQModule {

        @Override
        public void configure(Binder binder) {
            JettyModule.extend(binder).addServlet(ContentServlet.class);
        }
    }
}
