package com.nhl.bootique.jetty;

import java.util.Set;

import javax.servlet.Servlet;

/**
 * @since 0.10
 */
public class MappedServlet {

	private Servlet servlet;
	private Set<String> urlPatterns;

	public MappedServlet(Servlet servlet, Set<String> urlPatterns) {
		this.servlet = servlet;
		this.urlPatterns = urlPatterns;
	}

	public Servlet getServlet() {
		return servlet;
	}

	/**
	 * @since 0.11
	 */
	public Set<String> getUrlPatterns() {
		return urlPatterns;
	}

	/**
	 * @deprecated since 0.11 use {@link #getUrlPattern()}.
	 */
	public String getPath() {
		
		if (getUrlPatterns().size() == 0) {
			return null;
		}
		
		if (getUrlPatterns().size() == 1) {
			return getUrlPatterns().iterator().next();
		}

		throw new UnsupportedOperationException(
				"This operation is deprecated and is not supported for servlets with multiple URL mappings");
	}
}
