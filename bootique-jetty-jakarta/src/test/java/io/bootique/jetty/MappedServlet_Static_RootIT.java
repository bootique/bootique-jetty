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

package io.bootique.jetty;

import io.bootique.BQCoreModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class MappedServlet_Static_RootIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void disabledByDefault() {
        testFactory.app("-s").run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        Response r = base.path("/other.txt").request().get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), r.getStatus());
    }

    @Test
    public void noResourceBase() {
        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addMappedServlet(MappedServlet.ofStatic("/").build()))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        Response r = base.path("/other.txt").request().get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), r.getStatus());
    }

    @Test
    public void staticResourceBase_FilePath() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addMappedServlet(MappedServlet.ofStatic("/").build());
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                            "src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
                })
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        // resources are mapped relative to "user.dir".
        Response r = base.path("/other.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void resourceBase_FilePath_AsServletParams() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addMappedServlet(MappedServlet.ofStatic("/").name("x").build());
                    BQCoreModule.extend(b).setProperty("bq.jetty.servlets.x.params.resourceBase",
                            "src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        Response r = base.path("/other.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void resourceBase_ServletParamsOverridesDefault() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addMappedServlet(MappedServlet.ofStatic("/")
                            .name("x")
                            .resourceBase("classpath:io/bootique/jetty/StaticResourcesIT_docroot/").build());
                    BQCoreModule.extend(b).setProperty(
                            "bq.jetty.servlets.x.params.resourceBase",
                            "src/test/resources/io/bootique/jetty/StaticResourcesIT_altdocroot/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        Response r = base.path("/other.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("alt other", r.readEntity(String.class));
    }

    @Test
    public void resourceBase_FilePathWithDotSlash() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addMappedServlet(MappedServlet.ofStatic("/").build());
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                            "./src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        // resources are mapped relative to "user.dir".
        Response r = base.path("/other.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void resourceBaseIsFileUrl() throws MalformedURLException {

        File baseDir = new File("src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
        String baseUrl = baseDir.getAbsoluteFile().toURI().toURL().toString();

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addMappedServlet(MappedServlet
                        .ofStatic("/")
                        .resourceBase(baseUrl)
                        .build()))
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/other.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void resourceBase_Classpath() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addMappedServlet(MappedServlet
                        .ofStatic("/")
                        .resourceBase("classpath:io/bootique/jetty/StaticResourcesIT_docroot")
                        .build()))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/other.txt").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void resourceBase_filePath_ImplicitIndex() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addMappedServlet(MappedServlet
                        .ofStatic("/")
                        .resourceBase("src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/")
                        .build()))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("<html><body><h2>Hi!</h2></body></html>", r.readEntity(String.class));
    }

    @Test
    public void resourceBase_classpath_ImplicitIndex() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).addMappedServlet(MappedServlet
                        .ofStatic("/")
                        .resourceBase("classpath:io/bootique/jetty/StaticResourcesIT_docroot/")
                        .build()))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("<html><body><h2>Hi!</h2></body></html>", r.readEntity(String.class));
    }
}
