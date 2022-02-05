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

package io.bootique.jetty.jakarta.server;

import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jetty.jakarta.JettyModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.message.GZipEncoder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@BQTest
public class CompressionIT {


    private static final String content = generateContent();
    private static final String contentLength_Uncompressed = String.valueOf(content.length());
    private static final String contentLength_Compressed = String.valueOf(compressedSize(content));

    static String generateContent() {

        // must be > 32 bytes. Shorter content is not compressed by Jetty by default

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            buf.append("content_stream_content_stream_");
        }

        return buf.toString();
    }

    static int compressedSize(String content) {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try {
            GZIPOutputStream zipper = new GZIPOutputStream(bytes);
            zipper.write(content.getBytes(StandardCharsets.UTF_8));
            zipper.close();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception compressing content", e);
        }

        return bytes.size();
    }

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private final WebTarget gzipTarget = ClientBuilder
            .newClient()
            .register(GZipEncoder.class)
            .target("http://localhost:8080/cs/");

    @Test
    public void testCompression_Flat() {

        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        Response flatResponse = gzipTarget.request().get();
        assertEquals(Status.OK.getStatusCode(), flatResponse.getStatus());
        assertEquals(content, flatResponse.readEntity(String.class));
        assertNull(flatResponse.getHeaderString("Content-Encoding"));
        assertEquals(contentLength_Uncompressed, flatResponse.getHeaderString("Content-Length"));
    }

    @Test
    public void testCompression_GzipDeflate() {
        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        Response gzipDeflateResponse = gzipTarget.request().acceptEncoding("gzip", "deflate").get();
        assertEquals(Status.OK.getStatusCode(), gzipDeflateResponse.getStatus());
        assertEquals(content, gzipDeflateResponse.readEntity(String.class));
        assertEquals("gzip", gzipDeflateResponse.getHeaderString("Content-Encoding"));
        assertEquals(contentLength_Compressed, gzipDeflateResponse.getHeaderString("Content-Length"));
    }

    @Test
    public void testCompression_Gzip() {
        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        Response gzipResponse = gzipTarget.request().acceptEncoding("gzip").get();
        assertEquals(Status.OK.getStatusCode(), gzipResponse.getStatus());
        assertEquals(content, gzipResponse.readEntity(String.class));
        assertEquals("gzip", gzipResponse.getHeaderString("Content-Encoding"));
        assertEquals(contentLength_Compressed, gzipResponse.getHeaderString("Content-Length"));
    }

    @Test
    public void testUncompressed() {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/jakarta/server/NoCompressionIT.yml")
                .module(new ServletModule())
                .run();

        Response flatResponse = gzipTarget.request().get();
        assertEquals(Status.OK.getStatusCode(), flatResponse.getStatus());
        assertEquals(content, flatResponse.readEntity(String.class));
        assertNull(flatResponse.getHeaderString("Content-Encoding"));

        Response gzipDeflateResponse = gzipTarget.request().acceptEncoding("gzip", "deflate").get();
        assertEquals(Status.OK.getStatusCode(), gzipDeflateResponse.getStatus());
        assertEquals(content, gzipDeflateResponse.readEntity(String.class));
        assertNull(gzipDeflateResponse.getHeaderString("Content-Encoding"));
    }

    static class ServletModule implements BQModule {

        @Override
        public void configure(Binder binder) {
            JettyModule.extend(binder).addServlet(ContentServlet.class);
        }

        @Provides
        ContentServlet createAnnotatedServlet() {
            return new ContentServlet();
        }

        @WebServlet(urlPatterns = "/cs/*")
        static class ContentServlet extends HttpServlet {

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.getWriter().append(content);
            }
        }
    }

}
