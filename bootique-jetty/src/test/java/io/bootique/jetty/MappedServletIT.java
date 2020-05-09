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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.servlet.Servlet;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MappedServletIT {

    private static final WebTarget target = ClientBuilder.newClient().target("http://localhost:8080");

    @RegisterExtension
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();
    private Servlet mockServlet;

    @BeforeEach
    public void before() {
        this.mockServlet = mock(Servlet.class);
    }

    @Test
    public void testMappedConfig() throws Exception {

        MappedServlet mappedServlet = new MappedServlet(mockServlet, new HashSet<>(Arrays.asList("/a/*", "/b/*")));

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addMappedServlet(mappedServlet))
                .run();

        Response r1 = target.path("/a").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());

        Response r2 = target.path("/b").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());

        Response r3 = target.path("/c").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r3.getStatus());

        verify(mockServlet, times(2)).service(any(), any());
    }

    @Test
    public void testMappedConfig_Override() throws Exception {

        MappedServlet mappedServlet = new MappedServlet(
                mockServlet,
                new HashSet<>(Arrays.asList("/a/*", "/b/*")),
                "s1");

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/MappedServletIT1.yml")
                .module(b -> JettyModule.extend(b).addMappedServlet(mappedServlet))
                .run();

        Response r1 = target.path("/a").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r1.getStatus());

        Response r2 = target.path("/b").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r2.getStatus());

        Response r3 = target.path("/c").request().get();
        assertEquals(Status.OK.getStatusCode(), r3.getStatus());

        verify(mockServlet).service(any(), any());
    }

}
