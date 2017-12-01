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

public class StaticResourcesIT {

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
    public void testEnabled_ButNoBase() {
        testFactory.app("-s")
                .module(b -> JettyModule.extend(b).useDefaultServlet())
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        Response r = base.path("/other.txt").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
    }

    @Test
    public void testWithContextBase_FilePath() {

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
    public void testWithContextBase_FileUrl() throws MalformedURLException {

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
    public void testWithContextBase_ClasspathUrl() {

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
    public void testWithContextBase_FilePath_ImplicitIndex() {

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

    @Test
    public void testWithContextBase_FilePath_DotSlash() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).useDefaultServlet();
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                            "./src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("<html><body><h2>Hi!</h2></body></html>", r.readEntity(String.class));
    }

    @Test
    public void testWithServletBase() {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/StaticResourcesIT_FilePath.yml")
                .module(b -> JettyModule.extend(b).useDefaultServlet())
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        Response r = base.path("/other.txt").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("I am a text file", r.readEntity(String.class));
    }

    @Test
    public void testContributeStaticServlet() {

        testFactory.app("-s")
                .module(b -> {
                    JettyModule.extend(b).addStaticServlet("sub", "/sub1/*", "/sub2/*");
                    BQCoreModule.extend(b).setProperty("bq.jetty.staticResourceBase",
                            "src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot_subfolders/");
                })
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        assertEquals(Status.NOT_FOUND.getStatusCode(), base.path("/").request().get().getStatus());
        assertEquals(Status.NOT_FOUND.getStatusCode(), base.path("/other.txt").request().get().getStatus());
        assertEquals(Status.NOT_FOUND.getStatusCode(), base.path("/sub3/other.txt").request().get().getStatus());

        Response r1 = base.path("/sub1/other.txt").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("other1", r1.readEntity(String.class));

        Response r2 = base.path("/sub2/other.txt").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("other2", r2.readEntity(String.class));

        Response r3 = base.path("/sub2/").request().get();
        assertEquals(Status.OK.getStatusCode(), r3.getStatus());
        assertEquals("<html><body><h2>2</h2></body></html>", r3.readEntity(String.class));
    }

}
