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

package io.bootique.jetty.jakarta.server;

import io.bootique.annotation.BQConfigProperty;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public abstract class WebArtifactFactory {

	private Map<String, String> params;
	private Set<String> urlPatterns;

	/**
	 * @param urlPatterns
	 *            a set of URL patterns for the servlet created by this factory.
	 */
	@BQConfigProperty
	public void setUrlPatterns(Set<String> urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	@BQConfigProperty
	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	protected Map<String, String> getParams(Map<String, String> mappedParams) {

		Map<String, String> params = this.params;
		if (params == null || params.isEmpty()) {
			params = mappedParams;
		}

		if (params == null) {
			params = Collections.emptyMap();
		}
		
		return params;
	}

	protected Set<String> getUrlPatterns(Set<String> mappedPatterns) {

		Set<String> urlPatterns = this.urlPatterns;
		if (urlPatterns == null || urlPatterns.isEmpty()) {
			urlPatterns = mappedPatterns;
		}

		if (urlPatterns == null) {
			urlPatterns = Collections.emptySet();
		}

		return urlPatterns;
	}

}
