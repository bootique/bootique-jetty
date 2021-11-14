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
package io.bootique.jetty.jakarta;

import io.bootique.BQCoreModule;
import io.bootique.command.CommandOutcome;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class StaticServletIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testCommonResourceBase() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addStaticServlet("sub", "/sub1/*", "/sub2/*");
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                            "src/test/resources/io/bootique/jetty/jakarta/StaticResourcesIT_docroot_subfolders/");
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

    @Test
    public void testResourcePathResolving() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b)
                            .addStaticServlet("s1", "/sub1/*")
                            .addStaticServlet("s2", "/sub2/*")
                            .addStaticServlet("s3", "/sub3/*")
                            .addStaticServlet("s4", "/sub4/*");

                    BQCoreModule.extend(b)

                            // shared resource base; some servlets use it implicitly, others override it
                            .setProperty("bq.jetty.staticResourceBase",
                                    "src/test/resources/io/bootique/jetty/jakarta/ResourcePathResolving/")

                            // s1: own resource base and "pathInfoOnly == false" (so servlet path is a part of the
                            // static folder path)
                            .setProperty("bq.jetty.servlets.s1.params.resourceBase",
                                    "src/test/resources/io/bootique/jetty/jakarta/ResourcePathResolving/root1/")

                            // s2: own resource base and "pathInfoOnly == true" (so servlet path is excluded from the
                            // static folder path)
                            .setProperty("bq.jetty.servlets.s2.params.resourceBase",
                                    "src/test/resources/io/bootique/jetty/jakarta/ResourcePathResolving/root2/")
                            .setProperty("bq.jetty.servlets.s2.params.pathInfoOnly", "true")

                            // s3: shared resource base and  "pathInfoOnly == false"
                            // ...

                            // s4: shared resource base and  "pathInfoOnly == true"
                            .setProperty("bq.jetty.servlets.s4.params.pathInfoOnly", "true");

                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/sub1/f.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("[/root1/sub1/f.txt]", r1.readEntity(String.class));

        Response r2 = base.path("/sub2/f.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("[/root2/f.txt]", r2.readEntity(String.class));

        Response r3 = base.path("/sub3/f.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r3.getStatus());
        assertEquals("[/sub3/f.txt]", r3.readEntity(String.class));

        Response r4 = base.path("/sub4/f.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r4.getStatus());
        assertEquals("[/f.txt]", r4.readEntity(String.class));
    }

    @Test
    public void testResourceBaseClasspath_Missing() {

        CommandOutcome run = testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addStaticServlet("s1", "/sub1/*");

                    BQCoreModule.extend(b)
                            .setProperty("bq.jetty.servlets.s1.params.resourceBase",
                                    "classpath:io/bootique/jetty/jakarta/no_such_folder/");
                })
                .run();

        assertTrue(run.isSuccess(), "failed to start with invalid folder");
    }

    @Test
    public void testResourceBaseClasspath() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addStaticServlet("s1", "/sub1/*");

                    BQCoreModule.extend(b)
                            // s1: own resource base specified as "classpath"
                            .setProperty("bq.jetty.servlets.s1.params.resourceBase",
                                    "classpath:io/bootique/jetty/jakarta/ResourcePathResolving/root1/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/sub1/f.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("[/root1/sub1/f.txt]", r1.readEntity(String.class));
    }
}
