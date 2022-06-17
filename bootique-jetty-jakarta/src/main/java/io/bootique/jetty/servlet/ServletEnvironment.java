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

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Provides access to servlet spec objects active at any particular moment.
 * Normally used by the services that operate in a context of a web request.
 * <p>
 * Take extra care to avoid inadvertently caching returned objects as they
 * should not be retained once they go out of scope of Jetty threads.
 */
public interface ServletEnvironment {

	Optional<ServletContext> context();

	/**
	 * Returns an optional for HttpServletRequest currently in progress. Will
	 * only return a non-empty Optional when invoked within an ongoing request
	 * thread.
	 * 
	 * @return an optional for HttpServletRequest currently in progress.
	 */
	Optional<HttpServletRequest> request();
}
