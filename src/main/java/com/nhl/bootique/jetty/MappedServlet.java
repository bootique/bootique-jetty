package com.nhl.bootique.jetty;

import javax.servlet.Servlet;

/**
 * @since 0.10
 */
public class MappedServlet {

	private Servlet servlet;
	private String path;

	public MappedServlet(Servlet servlet, String path) {
		this.servlet = servlet;
		this.path = path;
	}

	public Servlet getServlet() {
		return servlet;
	}

	public String getPath() {
		return path;
	}

}
