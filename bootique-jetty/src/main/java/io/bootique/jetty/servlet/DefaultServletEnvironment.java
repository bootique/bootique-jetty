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

package io.bootique.jetty.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

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
