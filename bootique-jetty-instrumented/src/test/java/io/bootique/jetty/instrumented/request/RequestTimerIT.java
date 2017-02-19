package io.bootique.jetty.instrumented.request;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.instrumented.unit.InstrumentedJettyApp;
import io.bootique.test.BQDaemonTestRuntime;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class RequestTimerIT {

	@Rule
	public InstrumentedJettyApp app = new InstrumentedJettyApp();

	@Test
	public void testInitParametersPassed() {

		BQDaemonTestRuntime runtime = app.start(
				binder -> JettyModule.extend(binder).addServlet(new TestServlet(), "s1", "/*"));

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r1 = base.path("/").request().get();
		assertEquals(Status.OK.getStatusCode(), r1.getStatus());

		assertEquals("test_servlet", r1.readEntity(String.class));

		MetricRegistry metrics = runtime.getRuntime().getInstance(MetricRegistry.class);

		Collection<Timer> timers = metrics.getTimers().values();
		assertEquals(1, timers.size());

		Timer timer = timers.iterator().next();
		assertEquals(1, timer.getCount());

		base.path("/").request().get().close();
		assertEquals(2, timer.getCount());
	}

	static class TestServlet extends HttpServlet {
		private static final long serialVersionUID = -3190255883516320766L;

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");
			resp.getWriter().print("test_servlet");
		}
	}

}
