package com.nhl.bootique.jetty.server;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhl.bootique.jetty.MappedFilter;

/**
 * @since 0.13
 */
public class FilterFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(FilterFactory.class);

	private Map<String, String> params;

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public Optional<FilterHolder> createAndAddJettyFilter(ServletContextHandler handler, MappedFilter mappedFilter) {

		Objects.requireNonNull(mappedFilter.getFilter());

		if (mappedFilter.getUrlPatterns().isEmpty()) {
			// TODO: old Java anti-pattern is to use filters for the sake
			// of their "init" method, so perhaps we do need to add unmapped
			// filter after all?
			LOGGER.info("Skipping unmapped filter {}", mappedFilter.getFilter().getClass().getName());
			return Optional.empty();
		}

		FilterHolder holder = new FilterHolder(mappedFilter.getFilter());

		if (mappedFilter.getName() != null) {
			holder.setName(mappedFilter.getName());
		}

		if (params != null) {
			params.forEach((k, v) -> holder.setInitParameter(k, v));
		}

		EnumSet<DispatcherType> dispatches = EnumSet.of(DispatcherType.REQUEST);

		mappedFilter.getUrlPatterns().forEach(urlPattern -> {

			if (LOGGER.isInfoEnabled()) {
				String name = mappedFilter.getName() != null ? mappedFilter.getName() : "?";
				LOGGER.info("Adding filter '{}' mapped to {}", name, urlPattern);
			}
			
			handler.addFilter(holder, urlPattern, dispatches);
		});

		return Optional.of(holder);
	}

}
