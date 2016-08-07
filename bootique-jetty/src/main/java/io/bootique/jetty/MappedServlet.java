package io.bootique.jetty;

import javax.servlet.Servlet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @since 0.10
 */
public class MappedServlet extends MappedWebArtifact<Servlet> {

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
		this(servlet, urlPatterns, name, Collections.emptyMap());
	}

	/**
	 * @since 0.15
	 * @param servlet
	 *            underlying servlet instance.
	 * @param urlPatterns
	 *            URL patterns that this servlet will respond to.
	 * @param name
	 *            servlet name. If null, Jetty will assign its own name.
	 * @param params
	 *            servlet init parameters map.
	 */
	public MappedServlet(Servlet servlet, Set<String> urlPatterns, String name, Map<String, String> params) {
		super(servlet, urlPatterns, name, params);
	}

	public Servlet getServlet() {
		return getArtifact();
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
}
