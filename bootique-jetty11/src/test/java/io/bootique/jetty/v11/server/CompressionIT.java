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

package io.bootique.jetty.v11.server;

import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jetty.v11.JettyModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.glassfish.jersey.message.GZipEncoder;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@BQTest
public class CompressionIT {

    // must be big enough.. compression on small strings is skipped
    private static final String OUT_CONTENT;
    static {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<100; i++) {
            sb.append("content_stream_content_stream_");
        }
        OUT_CONTENT = sb.toString();
    }

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private WebTarget gzipTarget = ClientBuilder
            .newClient()
            .register(GZipEncoder.class)
            .target("http://localhost:8080/cs/");

    @Test
    public void testCompression_Flat()  {

        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        Response flatResponse = gzipTarget.request().get();
        assertEquals(Status.OK.getStatusCode(), flatResponse.getStatus());
        assertEquals(OUT_CONTENT, flatResponse.readEntity(String.class));
        assertNull(flatResponse.getHeaderString("Content-Encoding"));
    }

    @Test
    public void testCompression_GzipDeflate() {
        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        Response gzipDeflateResponse = gzipTarget.request().acceptEncoding("gzip", "deflate").get();
        assertEquals(Status.OK.getStatusCode(), gzipDeflateResponse.getStatus());
        assertEquals(OUT_CONTENT, gzipDeflateResponse.readEntity(String.class));
        assertEquals("gzip", gzipDeflateResponse.getHeaderString("Content-Encoding"));
    }

    @Test
    public void testCompression_Gzip() throws Exception {
        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        Response gzipResponse = gzipTarget.request().acceptEncoding("gzip").get();
        assertEquals(Status.OK.getStatusCode(), gzipResponse.getStatus());
        assertEquals(OUT_CONTENT, gzipResponse.readEntity(String.class));
        assertEquals("gzip", gzipResponse.getHeaderString("Content-Encoding"));
    }

    @Test
    public void testUncompressed() {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/v11/server/NoCompressionIT.yml")
                .module(new ServletModule())
                .run();

        Response flatResponse = gzipTarget.request().get();
        assertEquals(Status.OK.getStatusCode(), flatResponse.getStatus());
        assertEquals(OUT_CONTENT, flatResponse.readEntity(String.class));
        assertNull(flatResponse.getHeaderString("Content-Encoding"));

        Response gzipDeflateResponse = gzipTarget.request().acceptEncoding("gzip", "deflate").get();
        assertEquals(Status.OK.getStatusCode(), gzipDeflateResponse.getStatus());
        assertEquals(OUT_CONTENT, gzipDeflateResponse.readEntity(String.class));
        assertNull(gzipDeflateResponse.getHeaderString("Content-Encoding"));
    }

    class ServletModule implements BQModule {

        @Override
        public void configure(Binder binder) {
            JettyModule.extend(binder).addServlet(ContentServlet.class);
        }

        @Provides
        ContentServlet createAnnotatedServlet() {
            return new ContentServlet();
        }

        @WebServlet(urlPatterns = "/cs/*")
        class ContentServlet extends HttpServlet {

            private static final long serialVersionUID = -8896839263652092254L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                resp.getWriter().append(OUT_CONTENT);
            }
        }
    }

}
