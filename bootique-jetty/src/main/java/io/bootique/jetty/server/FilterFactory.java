/**
 *  Licensed to ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.jetty.server;

import io.bootique.annotation.BQConfig;
import io.bootique.jetty.MappedFilter;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@BQConfig
public class FilterFactory extends WebArtifactFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(FilterFactory.class);

	public Optional<FilterHolder> createAndAddJettyFilter(ServletContextHandler handler, MappedFilter mappedFilter) {

		Objects.requireNonNull(mappedFilter.getFilter());

		Set<String> urlPatterns = getUrlPatterns(mappedFilter.getUrlPatterns());

		if (urlPatterns.isEmpty()) {
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

		Map<String, String> params = getParams(mappedFilter.getParams());
		if (params != null) {
			params.forEach(holder::setInitParameter);
		}

		EnumSet<DispatcherType> dispatches = EnumSet.of(DispatcherType.REQUEST);

		urlPatterns.forEach(urlPattern -> {

			if (LOGGER.isInfoEnabled()) {
				String name = mappedFilter.getName() != null ? mappedFilter.getName() : "?";
				LOGGER.info("Adding filter '{}' mapped to {}", name, urlPattern);
			}

			handler.addFilter(holder, urlPattern, dispatches);
		});

		return Optional.of(holder);
	}

}
