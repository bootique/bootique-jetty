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

import io.bootique.jetty.MappedServlet;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * A factory that analyzes Servlet annotations to create a {@link MappedServlet} out of Servlet.
 */
public class MappedServletFactory {

	public MappedServlet toMappedServlet(Servlet servlet) {

		WebServlet wsAnnotation = servlet.getClass().getAnnotation(WebServlet.class);

		if (wsAnnotation == null) {
			throw new IllegalArgumentException(
					"Servlet contains no @WebServlet annotation and can not be mapped directly. Wrap it in a MappedServlet instead.");
		}

		String name = wsAnnotation.name() != null && wsAnnotation.name().length() > 0 ? wsAnnotation.name() : null;
		Set<String> urlPatterns = new HashSet<>(asList(wsAnnotation.urlPatterns()));

		Map<String, String> initParams = new HashMap<>();

		WebInitParam[] paramsArray = wsAnnotation.initParams();
		if (paramsArray != null) {
			asList(paramsArray).forEach(p -> initParams.put(p.name(), p.value()));
		}

		return new MappedServlet(servlet, urlPatterns, name, initParams);
	}

}
