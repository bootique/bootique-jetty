package com.nhl.bootique.jetty.server;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;

import com.nhl.bootique.jetty.MappedServlet;

/**
 * A factory that analyzes Servlet annotations to create a {@link MappedServlet}
 * out of Servlet.
 * 
 * @since 0.14
 */
public class MappedServletFactory {

	public MappedServlet toMappedServlet(Servlet servlet) {

		WebServlet wsAnnotation = servlet.getClass().getAnnotation(WebServlet.class);

		if (wsAnnotation == null) {
			throw new IllegalArgumentException(
					"Servlet contains no @WebServlet annotation and can not be mapped directly");
		}
		
		String name = wsAnnotation.name() != null && wsAnnotation.name().length() > 0 ? wsAnnotation.name() : null;
		Set<String> urlPatterns = new HashSet<>(asList(wsAnnotation.urlPatterns()));
		return new MappedServlet(servlet, urlPatterns, name);
	}

}
