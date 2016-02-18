package com.nhl.bootique.jetty.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

public class DefaultServletContainerStateTest {

	@Test
	public void testContext() {
		DefaultServletContainerState o = new DefaultServletContainerState();

		assertFalse(o.context().isPresent());

		ServletContext mockContext = mock(ServletContext.class);
		o.contextInitialized(new ServletContextEvent(mockContext));
		assertSame(mockContext, o.context().get());

		o.contextDestroyed(new ServletContextEvent(mockContext));
		assertFalse(o.context().isPresent());
	}

	@Test
	public void testRequest() {
		DefaultServletContainerState o = new DefaultServletContainerState();

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
