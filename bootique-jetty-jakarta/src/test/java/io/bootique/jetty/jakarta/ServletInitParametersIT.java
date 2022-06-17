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

package io.bootique.jetty.jakarta;

import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class ServletInitParametersIT {

	private static final WebTarget target = ClientBuilder.newClient().target("http://localhost:8080");

	@BQTestTool
	final BQTestFactory testFactory = new BQTestFactory();

	@Test
	@DisplayName("Params passed from YAML")
	public void testInitParametersPassed() {

		testFactory.app("-s", "-c", "classpath:io/bootique/jetty/jakarta/ServletInitParametersIT.yml")
				.autoLoadModules()
				.module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "s1", "/*"))
				.run();

		Response r1 = target.path("/").request().get();
		assertEquals(Status.OK.getStatusCode(), r1.getStatus());

		assertEquals("s1_a1_b2", r1.readEntity(String.class));
	}

	@Test
	@DisplayName("Params passed via module extender")
	public void testInitParametersPassed_Extender() {

		testFactory.app("-s")
				.autoLoadModules()
				.module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "s1", "/*"))
				.module(b -> JettyModule.extend(b).setServletParam("s1", "a", "a1").setServletParam("s1", "b", "b2"))
				.run();

		Response r1 = target.path("/").request().get();
		assertEquals(Status.OK.getStatusCode(), r1.getStatus());

		assertEquals("s1_a1_b2", r1.readEntity(String.class));
	}


	static class TestServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");

			ServletConfig config = getServletConfig();

			resp.getWriter().print(config.getServletName());
			resp.getWriter().print("_" + config.getInitParameter("a"));
			resp.getWriter().print("_" + config.getInitParameter("b"));
		}
	}
}
