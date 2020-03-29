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
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;

public class DefaultServletIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testDisabled() {
        testFactory.app("-s").run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        Response r = base.path("/other.txt").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
    }

    @Test
    public void testDefaultServlet_NoResourceBase() {
        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).useDefaultServlet())
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        Response r = base.path("/other.txt").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
    }

    @Test
    public void testDefaultServlet_ResourceBaseIsFilePath() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).useDefaultServlet();
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                            "src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
                })
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        // resources are mapped relative to "user.dir".
        Response r = base.path("/other.txt").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void testDefaultServlet_ResourceBaseIsFilePath_SetViaServletParams() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).useDefaultServlet();
                    BQCoreModule.extend(b).setProperty("bq.jetty.servlets.default.params.resourceBase",
                            "src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        Response r = base.path("/other.txt").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void testDefaultServlet_ResourceBaseIsFilePathWithDotSlash() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).useDefaultServlet();
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                            "./src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        // resources are mapped relative to "user.dir".
        Response r = base.path("/other.txt").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void testDefaultServlet_ResourceBaseIsFileUrl() throws MalformedURLException {

        File baseDir = new File("src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
        String baseUrl = baseDir.getAbsoluteFile().toURI().toURL().toString();

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).useDefaultServlet();
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase", baseUrl);
                })
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/other.txt").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void testDefaultServlet_ResourceBaseIsClasspathUrl() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).useDefaultServlet();
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                            "classpath:io/bootique/jetty/StaticResourcesIT_docroot");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/other.txt").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void testDefaultServlet_ResourceBaseIsFilePath_ImplicitIndex() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).useDefaultServlet();
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                            "src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("<html><body><h2>Hi!</h2></body></html>", r.readEntity(String.class));
    }
}
