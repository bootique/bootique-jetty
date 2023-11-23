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

import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.message.GZipEncoder;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class MaxFormSettingsIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    private WebTarget target = ClientBuilder.newClient().register(GZipEncoder.class)
            .target("http://localhost:8080/");

    @Test
    public void maxFormContentSize() {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/server/MaxFormSettingsIT_10b_request.yml")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
                .run();

        Response belowThreshold = target
                .request()
                .post(Entity.entity("a=1234567", MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        assertEquals(200, belowThreshold.getStatus());
        assertEquals("[1234567]", belowThreshold.readEntity(String.class));

        Response aboveThreshold = target
                .request()
                .post(Entity.entity("a=123456789", MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        assertEquals(400, aboveThreshold.getStatus());

        // the size limit includes the full form with keys and "=" signs...
        Response atThreshold = target
                .request()
                .post(Entity.entity("a=12345678", MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        assertEquals(200, atThreshold.getStatus());
        assertEquals("[12345678]", atThreshold.readEntity(String.class));
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String a = req.getParameter("a");
            resp.getWriter().append("[" + a + "]");
        }
    }
}
