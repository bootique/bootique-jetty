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

package io.bootique.jetty.v11;

import jakarta.servlet.Servlet;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class MappedServlet<T extends Servlet> extends MappedWebArtifact<T> {

	public MappedServlet(T servlet, Set<String> urlPatterns) {
		this(servlet, urlPatterns, null);
	}

	/**
	 * @param servlet
	 *            underlying servlet instance.
	 * @param urlPatterns
	 *            URL patterns that this servlet will respond to.
	 * @param name
	 *            servlet name. If null, Jetty will assign its own name.
	 */
	public MappedServlet(T servlet, Set<String> urlPatterns, String name) {
		this(servlet, urlPatterns, name, Collections.emptyMap());
	}

	/**
	 * @param servlet
	 *            underlying servlet instance.
	 * @param urlPatterns
	 *            URL patterns that this servlet will respond to.
	 * @param name
	 *            servlet name. If null, Jetty will assign its own name.
	 * @param params
	 *            servlet init parameters map.
	 */
	public MappedServlet(T servlet, Set<String> urlPatterns, String name, Map<String, String> params) {
		super(servlet, urlPatterns, name, params);
	}

	public T getServlet() {
		return getArtifact();
	}
}
