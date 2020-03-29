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
package io.bootique.jetty;

import io.bootique.BQCoreModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class StaticServletIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testContributeStaticServlet() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addStaticServlet("sub", "/sub1/*", "/sub2/*");
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                            "src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot_subfolders/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), base.path("/").request().get().getStatus());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), base.path("/other.txt").request().get().getStatus());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), base.path("/sub3/other.txt").request().get().getStatus());

        Response r1 = base.path("/sub1/other.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("other1", r1.readEntity(String.class));

        Response r2 = base.path("/sub2/other.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("other2", r2.readEntity(String.class));

        Response r3 = base.path("/sub2/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r3.getStatus());
        assertEquals("<html><body><h2>2</h2></body></html>", r3.readEntity(String.class));
    }
}
