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
public class MappedServlet_StaticIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    @Deprecated
    public void commonResourceBase_addStaticServlet() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addMappedServlet(MappedServlet.ofStatic("/sub1/*", "/sub2/*").name("sub").build());
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

    @Test
    public void commonResourceBase() {

        testFactory.app("-s")
                .module(b -> {
                    MappedServlet<?> s = MappedServlet.ofStatic("/sub1/*", "/sub2/*").build();
                    JettyModule.extend(b).addMappedServlet(s);
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

    @Test
    public void unnamed() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addMappedServlet(MappedServlet
                                .ofStatic("/sub1/*")
                                .resourceBase("classpath:io/bootique/jetty/StaticResourcesIT_docroot_subfolders/")
                                .build())
                        .addMappedServlet(MappedServlet
                                .ofStatic("/sub2/*")
                                .resourceBase("classpath:io/bootique/jetty/StaticResourcesIT_docroot_subfolders/")
                                .build()))
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
    }

    @Test
    @Deprecated
    public void resourcePathResolving_addStaticServlet() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b)
                            .addMappedServlet(MappedServlet.ofStatic("/sub1/*").name("s1").build())
                            .addMappedServlet(MappedServlet.ofStatic("/sub2/*").name("s2").build())
                            .addMappedServlet(MappedServlet.ofStatic("/sub3/*").name("s3").build())
                            .addMappedServlet(MappedServlet.ofStatic("/sub4/*").name("s4").build());

                    BQCoreModule.extend(b)

                            // shared resource base; some servlets use it implicitly, others override it
                            .setProperty("bq.jetty.staticResourceBase",
                                    "src/test/resources/io/bootique/jetty/ResourcePathResolving/")

                            // s1: own resource base and "pathInfoOnly == false" (so servlet path is a part of the
                            // static folder path)
                            .setProperty("bq.jetty.servlets.s1.params.resourceBase",
                                    "src/test/resources/io/bootique/jetty/ResourcePathResolving/root1/")

                            // s2: own resource base and "pathInfoOnly == true" (so servlet path is excluded from the
                            // static folder path)
                            .setProperty("bq.jetty.servlets.s2.params.resourceBase",
                                    "src/test/resources/io/bootique/jetty/ResourcePathResolving/root2/")
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
    public void builderParams() {

        testFactory.app("-s")
                .module(b -> {

                    BQCoreModule.extend(b)
                            // shared resource base; some servlets use it implicitly, others override it
                            .setProperty("bq.jetty.staticResourceBase",
                                    "src/test/resources/io/bootique/jetty/ResourcePathResolving/");

                    JettyModule.extend(b)
                            // s1: own resource base and "pathInfoOnly == false" (so servlet path is a part of the
                            // static folder path)
                            .addMappedServlet(MappedServlet.ofStatic("/sub1/*").resourceBase("classpath:io/bootique/jetty/ResourcePathResolving/root1/").build())

                            // s2: own resource base and "pathInfoOnly == true" (so servlet path is excluded from the
                            // static folder path)
                            .addMappedServlet(MappedServlet.ofStatic("/sub2/*").resourceBase("classpath:io/bootique/jetty/ResourcePathResolving/root2/").pathInfoOnly().build())

                            // s3: shared resource base and  "pathInfoOnly == false"
                            .addMappedServlet(MappedServlet.ofStatic("/sub3/*").build())

                            // s4: shared resource base and  "pathInfoOnly == true"
                            .addMappedServlet(MappedServlet.ofStatic("/sub4/*").pathInfoOnly().build());
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
    public void overrideFromParams() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b)
                            // this "pathInfo" should be overridden by params "pathInfo" of "false"
                            .addMappedServlet(MappedServlet.ofStatic("/sub1/*").name("s1").pathInfoOnly().build())

                            // this resource base should be overridden by params resource base
                            .addMappedServlet(MappedServlet.ofStatic("/sub2/*").name("s2").resourceBase("classpath:io/bootique/jetty/ResourcePathResolving/root1/").build())
                            .addMappedServlet(MappedServlet.ofStatic("/sub3/*").name("s3").build())
                            .addMappedServlet(MappedServlet.ofStatic("/sub4/*").name("s4").build());

                    BQCoreModule.extend(b)

                            // shared resource base; some servlets use it implicitly, others override it
                            .setProperty("bq.jetty.staticResourceBase",
                                    "src/test/resources/io/bootique/jetty/ResourcePathResolving/")

                            // s1: own resource base and "pathInfoOnly == false" (so servlet path is a part of the
                            // static folder path)
                            .setProperty("bq.jetty.servlets.s1.params.resourceBase",
                                    "src/test/resources/io/bootique/jetty/ResourcePathResolving/root1/")
                            .setProperty("bq.jetty.servlets.s1.params.pathInfoOnly", "false")

                            // s2: own resource base and "pathInfoOnly == true" (so servlet path is excluded from the
                            // static folder path)
                            .setProperty("bq.jetty.servlets.s2.params.resourceBase",
                                    "src/test/resources/io/bootique/jetty/ResourcePathResolving/root2/")
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
    public void resourceBaseClasspath_Missing() {

        CommandOutcome run = testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b)
                            .addMappedServlet(MappedServlet.ofStatic("/sub1/*").name("s1").build());

                    BQCoreModule.extend(b)
                            .setProperty("bq.jetty.servlets.s1.params.resourceBase",
                                    "classpath:io/bootique/jetty/no_such_folder/");
                })
                .run();

        assertTrue(run.isSuccess(), "failed to start with invalid folder");
    }

    @Test
    public void resourceBaseClasspath() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addMappedServlet(MappedServlet.ofStatic("/sub1/*").name("s1").build());

                    BQCoreModule.extend(b)
                            // s1: own resource base specified as "classpath"
                            .setProperty("bq.jetty.servlets.s1.params.resourceBase",
                                    "classpath:io/bootique/jetty/ResourcePathResolving/root1/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r1 = base.path("/sub1/f.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("[/root1/sub1/f.txt]", r1.readEntity(String.class));
    }
}
