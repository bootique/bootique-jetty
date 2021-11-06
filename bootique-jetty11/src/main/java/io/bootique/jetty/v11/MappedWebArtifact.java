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

import java.util.Map;
import java.util.Set;

public abstract class MappedWebArtifact<T> {

	private final T artifact;
	private final Set<String> urlPatterns;
	private final String name;
	private final Map<String, String> params;

	public MappedWebArtifact(T artifact, Set<String> urlPatterns, String name, Map<String, String> params) {
		this.artifact = artifact;
		this.name = name;
		this.urlPatterns = urlPatterns;
		this.params = params;
	}

	protected T getArtifact() {
		return artifact;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public Set<String> getUrlPatterns() {
		return urlPatterns;
	}

	/**
	 * @return an optional servlet name.
	 */
	public String getName() {
		return name;
	}

}