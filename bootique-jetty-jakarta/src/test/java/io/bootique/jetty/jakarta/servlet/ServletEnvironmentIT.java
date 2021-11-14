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

package io.bootique.jetty.jakarta.servlet;

import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.di.TypeLiteral;
import io.bootique.jetty.jakarta.JettyModule;
import io.bootique.jetty.jakarta.MappedServlet;
import io.bootique.jetty.jakarta.servlet.ServletEnvironment;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@BQTest
public class ServletEnvironmentIT {

	private Runnable assertion;

	@BQTestTool
	final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

	@AfterEach
	public void after() {
		assertion = null;
	}

	@Test
	public void testServletContainerState() {
		testFactory.app("-s").module(new ServletCheckingModule()).run();

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		assertNull(assertion);

		base.path("/a").request().get();
		Objects.requireNonNull(assertion).run();
		assertion = null;

		base.path("/a/1").request().get();
		Objects.requireNonNull(assertion).run();
		assertion = null;

		base.path("/a/2").request().get();
		Objects.requireNonNull(assertion).run();
	}

	class ServletCheckingModule implements BQModule {

		@Override
		public void configure(Binder binder) {
			TypeLiteral<MappedServlet<ServletCheckingState>> st = new TypeLiteral<MappedServlet<ServletCheckingState>>() {};
			JettyModule.extend(binder).addMappedServlet(st);
		}

		@Provides
		MappedServlet<ServletCheckingState> createMappedServlet(ServletCheckingState servlet) {
			return new MappedServlet(servlet, new HashSet<>(Arrays.asList("/a/*")));
		}

		@Provides
		ServletCheckingState createServlet(ServletEnvironment state) {
			return new ServletCheckingState(state);
		}

		class ServletCheckingState extends HttpServlet {

			private static final long serialVersionUID = -1713490500665580905L;
			private ServletEnvironment state;

			public ServletCheckingState(ServletEnvironment state) {
				this.state = state;
			}

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// capture variable values before sticking them in the
				// closure...
				HttpServletRequest stateRequest = state.request().get();
				ServletContext stateContext = state.context().get();
				ServletContext requestContext = req.getServletContext();

				assertion = () -> {
					assertSame(req, stateRequest);
					assertSame(requestContext, stateContext);
				};
			}
		}
	}

}
