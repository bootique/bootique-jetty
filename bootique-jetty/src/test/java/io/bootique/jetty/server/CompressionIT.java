package io.bootique.jetty.server;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.bootique.jetty.JettyModule;
import io.bootique.test.junit.BQTestFactory;
import org.glassfish.jersey.message.GZipEncoder;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CompressionIT {

    // must be big enough.. compression on small strings is skipped
    private static final String OUT_CONTENT = "content_stream_content_stream";

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

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

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/server/NoCompressionIT.yml")
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

    class ServletModule implements Module {

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
