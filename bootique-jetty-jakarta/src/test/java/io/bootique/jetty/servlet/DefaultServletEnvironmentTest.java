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

package io.bootique.jetty.servlet;

import io.bootique.jetty.servlet.DefaultServletEnvironment;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

public class DefaultServletEnvironmentTest {

	@Test
    public void context() {
		DefaultServletEnvironment o = new DefaultServletEnvironment();

		assertFalse(o.context().isPresent());

		ServletContext mockContext = mock(ServletContext.class);
		o.contextInitialized(new ServletContextEvent(mockContext));
		assertSame(mockContext, o.context().get());

		o.contextDestroyed(new ServletContextEvent(mockContext));
		assertFalse(o.context().isPresent());
	}

	@Test
    public void request() {
		DefaultServletEnvironment o = new DefaultServletEnvironment();

		assertFalse(o.request().isPresent());

		ServletContext mockContext = mock(ServletContext.class);
		ServletRequest mockRequest = mock(HttpServletRequest.class);
		o.requestInitialized(new ServletRequestEvent(mockContext, mockRequest));
		assertSame(mockRequest, o.request().get());
		
		o.requestDestroyed(new ServletRequestEvent(mockContext, mockRequest));
		assertFalse(o.request().isPresent());
		
		ServletRequest mockRequest2 = mock(HttpServletRequest.class);
		o.requestInitialized(new ServletRequestEvent(mockContext, mockRequest2));
		assertSame(mockRequest2, o.request().get());
		
		o.requestDestroyed(new ServletRequestEvent(mockContext, mockRequest2));
		assertFalse(o.request().isPresent());
		
		// TODO: test multithreaded scenario..
	}
}
