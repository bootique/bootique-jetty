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
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * This test is second-guessing Jetty (there's no Bootique functionality added). Just checking that common static
 * resources include the proper Content-Type header.
 */
@BQTest
public class DefaultServlet_ContentTypeIT {

    @BQApp
    final static BQRuntime app = Bootique.app("--server")
            .autoLoadModules()
            .module(b -> JettyModule.extend(b).useDefaultServlet())
            .module(b -> BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                    "src/test/resources/io/bootique/jetty/DefaultServlet_ContentTypeIT_docroot/"))
            .createRuntime();

    final WebTarget target = ClientBuilder.newClient().target("http://localhost:8080");

    @Test
    public void testTextPlain() {
        checkContentType("a.txt", "text/plain");
    }

    @Test
    public void testTextHtml() {
        checkContentType("a.html", "text/html");
        checkContentType("/", "text/html");
    }

    @Test
    public void testApplicationJson() {
        checkContentType("a.json", "application/json");
    }

    @Test
    public void testTextCss() {
        checkContentType("a.css", "text/css");
    }

    @Test
    public void testApplicationJS() {
        checkContentType("a.js", "application/javascript");
    }

    @Test
    public void testImagePng() {
        checkContentType("a.png", "image/png");
    }

    private void checkContentType(String path, String expectedContentType) {
        Response r = target.path(path).request().get();
        assertEquals(200, r.getStatus(), () -> "Error fetching " + path);
        assertEquals(expectedContentType, r.getHeaderString("Content-Type"));
    }
}
