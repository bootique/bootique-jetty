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
import org.eclipse.jetty.servlet.DefaultServlet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
@EnabledOnOs({OS.LINUX, OS.MAC})
public class DefaultServletSymlinkPathIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    static final String html = "<html><head><title>My</title></head></html>";
    static String symlinkPath;

    @BeforeAll
    static void initWebRoot(@TempDir File dir) throws IOException {
        File physicalDir = new File(dir, "a");
        physicalDir.mkdir();

        File htmlFile = new File(physicalDir, "my.html");
        Files.write(htmlFile.toPath(), html.getBytes());

        File dirLink = new File(dir, "a_link");
        Files.createSymbolicLink(dirLink.toPath(), physicalDir.toPath().toAbsolutePath());
        symlinkPath = dirLink.getAbsolutePath();
        assertTrue(symlinkPath.endsWith("a_link"), symlinkPath);
    }

    @Test
    public void testSymlinkPath_BootiqueDefaultServlet() {

        testFactory.app("-s")
                // DefaultServlet behaves differently from MultiBaseStaticServlet, where the resource base symlinks are resolved before servlet creation
                .module(b -> JettyModule.extend(b).useDefaultServlet())
                .module(b -> BQCoreModule.extend(b).setProperty("bq.jetty.servlets.default.params.resourceBase", symlinkPath))
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/my.html").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals(html, r.readEntity(String.class));
    }

    @Test
    public void testSymlinkPath_RawDefaultServlet() {

        testFactory.app("-s")
                // DefaultServlet behaves differently from MultiBaseStaticServlet, where the resource base symlinks are resolved before servlet creation
                .module(b -> JettyModule.extend(b).addServlet(new DefaultServlet(), "a", "/a/*"))
                .module(b -> BQCoreModule.extend(b).setProperty("bq.jetty.servlets.a.params.resourceBase", symlinkPath))
                .module(b -> BQCoreModule.extend(b).setProperty("bq.jetty.servlets.a.params.pathInfoOnly", "true"))
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/a/my.html").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals(html, r.readEntity(String.class));
    }
}
