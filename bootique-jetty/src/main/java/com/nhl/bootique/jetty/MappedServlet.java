package com.nhl.bootique.jetty;

import java.util.Set;

import javax.servlet.Servlet;

/**
 * @since 0.10
 */
public class MappedServlet {

	private Servlet servlet;
	private Set<String> urlPatterns;
	private String name;

	public MappedServlet(Servlet servlet, Set<String> urlPatterns) {
		this(servlet, urlPatterns, null);
	}

	/**
	 * @since 0.13
	 * @param servlet
	 *            underlying servlet instance.
	 * @param urlPatterns
	 *            URL patterns that this servlet will respond to.
	 * @param name
	 *            servlet name. If null, Jetty will assign its own name.
	 */
	public MappedServlet(Servlet servlet, Set<String> urlPatterns, String name) {
		this.servlet = servlet;
		this.name = name;
		this.urlPatterns = urlPatterns;
	}

	public Servlet getServlet() {
		return servlet;
	}

	/**
	 * @since 0.11
	 * @return collection of URL patterns matching this servlet.
	 */
	public Set<String> getUrlPatterns() {
		return urlPatterns;
	}

	/**
	 * @deprecated since 0.11 use {@link #getUrlPatterns()}.
	 * @return a single URL pattern for this servlet.
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

	/**
	 * @since 0.13
	 * @return an optional servlet name.
	 */
	public String getName() {
		return name;
	}
}
