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
import io.bootique.jetty.MappedServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
@BQConfig
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

		Map<String, String> params = getParams(mappedServlet.getParams());
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
