package io.bootique.jetty.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * @since 0.12
 */
public class DefaultServletEnvironment
		implements ServletContextListener, ServletRequestListener, ServletEnvironment {

	private ServletContext context;
	private ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();

	@Override
	public Optional<ServletContext> context() {
		return Optional.ofNullable(this.context);
	}

	@Override
	public Optional<HttpServletRequest> request() {
		return Optional.ofNullable(request.get());
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		this.context = sce.getServletContext();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		this.context = null;
	}

	@Override
	public void requestInitialized(ServletRequestEvent sre) {
		request.set((HttpServletRequest) sre.getServletRequest());
	}

	@Override
	public void requestDestroyed(ServletRequestEvent sre) {
		request.set(null);
	}
}
