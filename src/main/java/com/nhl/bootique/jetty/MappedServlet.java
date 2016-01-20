package com.nhl.bootique.jetty;

import javax.servlet.Servlet;

/**
 * @since 0.10
 */
public class MappedServlet {

	private Servlet servlet;
	private String urlPattern;

	public MappedServlet(Servlet servlet, String urlPattern) {
		this.servlet = servlet;
		this.urlPattern = urlPattern;
	}

	public Servlet getServlet() {
		return servlet;
	}

	/**
	 * @since 0.11
	 */
	public String getUrlPattern() {
		return urlPattern;
	}
	
	/**
	 * @deprecated since 0.11 use {@link #getUrlPattern()}.
	 */
	public String getPath() {
		return getUrlPattern();
	}
}
