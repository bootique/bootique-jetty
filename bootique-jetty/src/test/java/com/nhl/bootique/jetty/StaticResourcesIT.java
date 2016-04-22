package com.nhl.bootique.jetty;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.MalformedURLException;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Rule;
import org.junit.Test;

import com.nhl.bootique.BQCoreModule;
import com.nhl.bootique.jetty.unit.JettyApp;

public class StaticResourcesIT {

	@Rule
	public JettyApp app = new JettyApp();

	@Test
	public void testDisabled() {
		app.startServer();

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
		Response r = base.path("/other.txt").request().get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
	}

	@Test
	public void testEnabled_ButNoBase() {
		app.startServer(binder -> {
			JettyModule.contributeDefaultServlet(binder);
		});

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
		Response r = base.path("/other.txt").request().get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
	}

	@Test
	public void testWithContextBase_FilePath() {
		app.startServer(binder -> {
			JettyModule.contributeDefaultServlet(binder);
			BQCoreModule.contributeProperties(binder).addBinding("bq.jetty.staticResourceBase")
					.toInstance("src/test/resources/com/nhl/bootique/jetty/StaticResourcesIT_docroot/");
		});

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		// resources are mapped relative to "user.dir".
		Response r = base.path("/other.txt").request().get();
		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		assertEquals("I am a text file", r.readEntity(String.class));
	}

	@Test
	public void testWithContextBase_FileUrl() throws MalformedURLException {

		File baseDir = new File("src/test/resources/com/nhl/bootique/jetty/StaticResourcesIT_docroot/");
		String baseUrl = baseDir.getAbsoluteFile().toURI().toURL().toString();

		app.startServer(binder -> {
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

		app.startServer(binder -> {
			JettyModule.contributeDefaultServlet(binder);
			BQCoreModule.contributeProperties(binder).addBinding("bq.jetty.staticResourceBase")
					.toInstance("classpath:com/nhl/bootique/jetty/StaticResourcesIT_docroot");
		});

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r = base.path("/other.txt").request().get();
		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		assertEquals("I am a text file", r.readEntity(String.class));
	}

	@Test
	public void testWithContextBase_FilePath_ImplicitIndex() {
		app.startServer(binder -> {
			JettyModule.contributeDefaultServlet(binder);
			BQCoreModule.contributeProperties(binder).addBinding("bq.jetty.staticResourceBase")
					.toInstance("src/test/resources/com/nhl/bootique/jetty/StaticResourcesIT_docroot/");
		});

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r = base.path("/").request().get();
		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		assertEquals("<html><body><h2>Hi!</h2></body></html>", r.readEntity(String.class));
	}

	@Test
	public void testWithServletBase() {

		app.startServer(binder -> JettyModule.contributeDefaultServlet(binder),
				"--config=src/test/resources/com/nhl/bootique/jetty/StaticResourcesIT_FilePath.yml");

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
		Response r = base.path("/other.txt").request().get();
		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		assertEquals("I am a text file", r.readEntity(String.class));
	}

	@Test
	public void testContributeStaticServlet() {

		app.startServer(binder -> {
			JettyModule.contributeStaticServlet(binder, "sub", "/sub1/*", "/sub2/*");
			BQCoreModule.contributeProperties(binder).addBinding("bq.jetty.staticResourceBase")
					.toInstance("src/test/resources/com/nhl/bootique/jetty/StaticResourcesIT_docroot_subfolders/");
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
