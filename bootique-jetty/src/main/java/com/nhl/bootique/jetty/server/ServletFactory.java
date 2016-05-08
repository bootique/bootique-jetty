package com.nhl.bootique.jetty.server;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhl.bootique.jetty.MappedServlet;

/**
 * @since 0.13
 */
public class ServletFactory extends WebArtifactFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServletFactory.class);

	public Optional<ServletHolder> createAndAddJettyServlet(ServletContextHandler handler,
			MappedServlet mappedServlet) {

		Objects.requireNonNull(mappedServlet.getServlet());

		Set<String> urlPatterns = getUrlPatterns(mappedServlet.getUrlPatterns());

		if (urlPatterns.isEmpty()) {

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

		Map<String, String> params = getParams();
		if (params != null) {
			params.forEach((k, v) -> holder.setInitParameter(k, v));
		}

		urlPatterns.forEach(urlPattern -> {

			if (LOGGER.isInfoEnabled()) {
				String name = mappedServlet.getName() != null ? mappedServlet.getName() : "?";
				LOGGER.info("Adding servlet '{}' mapped to {}", name, urlPattern);
			}

			handler.addServlet(holder, urlPattern);
		});

		return Optional.of(holder);
	}
}
