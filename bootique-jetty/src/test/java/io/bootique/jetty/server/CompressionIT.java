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

package io.bootique.jetty.server;

import io.bootique.BQCoreModule;
import io.bootique.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedServlet;
import io.bootique.junit.BQTest;
import io.bootique.junit.BQTestFactory;
import io.bootique.junit.BQTestTool;
import io.bootique.resource.ResourceFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;
import org.glassfish.jersey.message.GZipEncoder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class CompressionIT {

    private static final String content = readContent();
    private static final String contentLength_Uncompressed = String.valueOf(content.length());

    static String readContent() {

        URL contentUrl = new ResourceFactory("classpath:io/bootique/jetty/CompressionIT_docroot/index.html").getUrl();
        try (InputStream in = contentUrl.openStream()) {

            byte[] data = new byte[in.available()];
            in.read(data, 0, data.length);
            return new String(data, 0, data.length, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] gzip(String content) {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (GZIPOutputStream zipper = new GZIPOutputStream(bytes)) {
            zipper.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception compressing content", e);
        }

        return bytes.toByteArray();
    }

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private final WebTarget target = ClientBuilder
            .newClient()
            .register(GZipEncoder.class)
            .target("http://localhost:8080/");

    // a client that does not decode compressed responses, so that the compressed bytes can be inspected directly
    private final WebTarget rawTarget = ClientBuilder
            .newClient()
            .target("http://localhost:8080/");

    @Test
    public void compression_Flat() {

        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        Response flatResponse = target.path("cs").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), flatResponse.getStatus());
        assertEquals(content, flatResponse.readEntity(String.class));
        assertNull(flatResponse.getHeaderString("Content-Encoding"));
        assertEquals(contentLength_Uncompressed, flatResponse.getHeaderString("Content-Length"));
    }

    @Test
    public void compression_GzipDeflate() {
        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        Response gzipDeflateResponse = target.path("cs").request().acceptEncoding("gzip", "deflate").get();
        assertEquals(Response.Status.OK.getStatusCode(), gzipDeflateResponse.getStatus());
        assertEquals(content, gzipDeflateResponse.readEntity(String.class));
        assertEquals("gzip", gzipDeflateResponse.getHeaderString("Content-Encoding"));
    }

    @Test
    public void compression_Gzip() {
        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        Response gzipResponse = target.path("cs").request().acceptEncoding("gzip").get();
        assertEquals(Response.Status.OK.getStatusCode(), gzipResponse.getStatus());
        assertEquals(content, gzipResponse.readEntity(String.class));
        assertEquals("gzip", gzipResponse.getHeaderString("Content-Encoding"));
    }

    @Test
    public void compression_Gzip_RawBytes() {
        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        // checking the raw bytes on the wire, as CompressionHandler streams compressed responses without
        // a "Content-Length" header
        Response gzipResponse = rawTarget.path("cs").request().acceptEncoding("gzip").get();
        assertEquals(Response.Status.OK.getStatusCode(), gzipResponse.getStatus());
        assertEquals("gzip", gzipResponse.getHeaderString("Content-Encoding"));

        byte[] compressed = gzipResponse.readEntity(byte[].class);
        assertTrue(compressed.length < content.length(), "Compressed response is not smaller than the raw content");
        assertEquals((byte) 0x1f, compressed[0], "Not a gzip stream");
        assertEquals((byte) 0x8b, compressed[1], "Not a gzip stream");
    }

    @Test
    public void noRequestDecompression() {
        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        byte[] compressedRequest = gzip(content);

        // "Content-Encoding" must be passed via the Entity's Variant. Also must use "rawTarget".
        Response response = rawTarget.path("cs").request()
                .post(Entity.entity(compressedRequest,
                        new Variant(MediaType.APPLICATION_OCTET_STREAM_TYPE, (String) null, "gzip")));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // the deprecated GzipHandler did not inflate request bodies unless explicitly configured to do so, and
        // the CompressionHandler that replaced it is configured to preserve that behavior. So the servlet must
        // see the still-compressed bytes
        assertEquals(String.valueOf(compressedRequest.length), response.readEntity(String.class));
    }

    @Test
    public void compression_UnsupportedEncoding() {
        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        // only "gzip" is registered with the CompressionHandler, so "br" must fall back to an uncompressed response
        Response brResponse = target.path("cs").request().acceptEncoding("br").get();
        assertEquals(Response.Status.OK.getStatusCode(), brResponse.getStatus());
        assertEquals(content, brResponse.readEntity(String.class));
        assertNull(brResponse.getHeaderString("Content-Encoding"));
    }

    @Test
    public void uncompressed() {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/server/NoCompressionIT.yml")
                .module(new ServletModule())
                .run();

        Response flatResponse = target.path("cs").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), flatResponse.getStatus());
        assertEquals(content, flatResponse.readEntity(String.class));
        assertNull(flatResponse.getHeaderString("Content-Encoding"));

        Response gzipDeflateResponse = target.path("cs").request().acceptEncoding("gzip", "deflate").get();
        assertEquals(Response.Status.OK.getStatusCode(), gzipDeflateResponse.getStatus());
        assertEquals(content, gzipDeflateResponse.readEntity(String.class));
        assertNull(gzipDeflateResponse.getHeaderString("Content-Encoding"));
    }

    @Test
    public void compression_Gzip_DefaultServlet() {
        testFactory.app("-s")
                .module(new ServletModule())
                .run();

        Response gzipResponse = target.path("/").request().acceptEncoding("gzip").get();
        assertEquals(Response.Status.OK.getStatusCode(), gzipResponse.getStatus());
        assertEquals(content, gzipResponse.readEntity(String.class));
        assertEquals("gzip", gzipResponse.getHeaderString("Content-Encoding"));
    }


    static class ServletModule implements BQModule {

        @Override
        public void configure(Binder binder) {
            BQCoreModule
                    .extend(binder)
                    .setProperty("bq.jetty.staticResourceBase", "classpath:io/bootique/jetty/CompressionIT_docroot");

            JettyModule
                    .extend(binder)
                    .addServlet(ContentServlet.class)
                    .addMappedServlet(MappedServlet.ofStatic("/").name("default").build());
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

            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                // reporting the size of the body as seen by the servlet, to detect whether Jetty inflated it
                resp.getWriter().append(String.valueOf(req.getInputStream().readAllBytes().length));
            }
        }
    }


}
