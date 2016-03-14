package com.nhl.bootique.jetty.server;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhl.bootique.jetty.MappedServlet;

/**
 * @since 0.13
 */
public class ServletFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServletFactory.class);

	private Map<String, String> params;

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public Optional<ServletHolder> createAndAddJettyServlet(ServletContextHandler handler,
			MappedServlet mappedServlet) {
		Objects.requireNonNull(mappedServlet.getServlet());

		if (mappedServlet.getUrlPatterns().isEmpty()) {

			// TODO: old Java anti-pattern is to use servlets for the sake
			// of their "init" method, so perhaps we do need to add unampped
			// servlets after all?
			LOGGER.info("Skipping unmapped servlet {}", mappedServlet.getServlet().getClass().getName());
			return Optional.empty();
		}

		ServletHolder holder = new ServletHolder(mappedServlet.getServlet());

		if (mappedServlet.getName() != null) {
			holder.setName(mappedServlet.getName());
		}

		if (params != null) {
			params.forEach((k, v) -> holder.setInitParameter(k, v));
		}

		mappedServlet.getUrlPatterns().forEach(urlPattern -> {
			LOGGER.info("Adding servlet mapped to {}", urlPattern);
			handler.addServlet(holder, urlPattern);
		});

		return Optional.of(holder);
	}
}
