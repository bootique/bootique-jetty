package io.bootique.jetty.server;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import io.bootique.jetty.MappedServlet;

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
					"Servlet contains no @WebServlet annotation and can not be mapped directly. Wrap it in a MappedServlet instead.");
		}

		String name = wsAnnotation.name() != null && wsAnnotation.name().length() > 0 ? wsAnnotation.name() : null;
		Set<String> urlPatterns = new HashSet<>(asList(wsAnnotation.urlPatterns()));

		Map<String, String> initParams = new HashMap<>();

		WebInitParam[] paramsArray = wsAnnotation.initParams();
		if (paramsArray != null) {
			asList(paramsArray).forEach(p -> initParams.put(p.name(), p.value()));
		}

		return new MappedServlet(servlet, urlPatterns, name, initParams);
	}

}
