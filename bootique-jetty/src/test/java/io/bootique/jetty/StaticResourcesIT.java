package io.bootique.jetty;

import io.bootique.BQCoreModule;
import io.bootique.jetty.unit.JettyApp;
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
	public JettyApp app = new JettyApp();

	@Test
	public void testDisabled() {
		app.start();

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
		Response r = base.path("/other.txt").request().get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
	}

	@Test
	public void testEnabled_ButNoBase() {
		app.start(binder -> {
			JettyModule.contributeDefaultServlet(binder);
		});

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
		Response r = base.path("/other.txt").request().get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
	}

	@Test
	public void testWithContextBase_FilePath() {
		app.start(binder -> {
			JettyModule.contributeDefaultServlet(binder);
			BQCoreModule.contributeProperties(binder).addBinding("bq.jetty.staticResourceBase")
					.toInstance("src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
		});

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

		app.start(binder -> {
			JettyModule.contributeDefaultServlet(binder);
			BQCoreModule.contributeProperties(binder).addBinding("bq.jetty.staticResourceBase").toInstance(baseUrl);
		});

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r = base.path("/other.txt").request().get();
		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		assertEquals("I am a text file", r.readEntity(String.class));
	}

	@Test
	public void testWithContextBase_ClasspathUrl() {

		app.start(binder -> {
			JettyModule.contributeDefaultServlet(binder);
			BQCoreModule.contributeProperties(binder).addBinding("bq.jetty.staticResourceBase")
					.toInstance("classpath:io/bootique/jetty/StaticResourcesIT_docroot");
		});

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r = base.path("/other.txt").request().get();
		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		assertEquals("I am a text file", r.readEntity(String.class));
	}

	@Test
	public void testWithContextBase_FilePath_ImplicitIndex() {
		app.start(binder -> {
			JettyModule.contributeDefaultServlet(binder);
			BQCoreModule.contributeProperties(binder).addBinding("bq.jetty.staticResourceBase")
					.toInstance("src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
		});

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r = base.path("/").request().get();
		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		assertEquals("<html><body><h2>Hi!</h2></body></html>", r.readEntity(String.class));
	}
	
	@Test
	public void testWithContextBase_FilePath_DotSlash() {
		app.start(binder -> {
			JettyModule.contributeDefaultServlet(binder);
			BQCoreModule.contributeProperties(binder).addBinding("bq.jetty.staticResourceBase")
					.toInstance("./src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot/");
		});

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r = base.path("/").request().get();
		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		assertEquals("<html><body><h2>Hi!</h2></body></html>", r.readEntity(String.class));
	}

	@Test
	public void testWithServletBase() {

		app.start(binder -> JettyModule.contributeDefaultServlet(binder),
				"--config=src/test/resources/io/bootique/jetty/StaticResourcesIT_FilePath.yml");

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
		Response r = base.path("/other.txt").request().get();
		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		assertEquals("I am a text file", r.readEntity(String.class));
	}

	@Test
	public void testContributeStaticServlet() {

		app.start(binder -> {
			JettyModule.contributeStaticServlet(binder, "sub", "/sub1/*", "/sub2/*");
			BQCoreModule.contributeProperties(binder).addBinding("bq.jetty.staticResourceBase")
					.toInstance("src/test/resources/io/bootique/jetty/StaticResourcesIT_docroot_subfolders/");
		});

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
