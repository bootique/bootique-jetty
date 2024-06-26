/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jetty;

import io.bootique.jetty.servlet.MultiBaseStaticServlet;
import io.bootique.resource.FolderResourceFactory;
import jakarta.servlet.Servlet;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A wrapper around a servlet object that provides access to its URL mapping and parameters.
 */
public class MappedServlet<T extends Servlet> extends MappedWebArtifact<T> {

    /**
     * Starts a builder of a MappedServlet that will act as a web server serving static files from some folder. If
     * the "urlPatterns" parameter is null or empty, the root pattern ("/") is assumed.
     *
     * @since 3.0
     */
    public static StaticMappedServletBuilder ofStatic(String... urlPatterns) {
        Set<String> normalized = urlPatterns != null && urlPatterns.length > 0 ? Set.of(urlPatterns) : Set.of("/");
        return new StaticMappedServletBuilder(normalized);
    }

    public MappedServlet(T servlet, Set<String> urlPatterns) {
        this(servlet, urlPatterns, null);
    }

    /**
     * @param servlet     underlying servlet instance.
     * @param urlPatterns URL patterns that this servlet will respond to.
     * @param name        servlet name. If null, Jetty will assign its own name.
     */
    public MappedServlet(T servlet, Set<String> urlPatterns, String name) {
        this(servlet, urlPatterns, name, Collections.emptyMap());
    }

    /**
     * @param servlet     underlying servlet instance.
     * @param urlPatterns URL patterns that this servlet will respond to.
     * @param name        servlet name. If null, Jetty will assign its own name.
     * @param params      servlet init parameters map.
     */
    public MappedServlet(T servlet, Set<String> urlPatterns, String name, Map<String, String> params) {
        super(servlet, urlPatterns, name, params);
    }

    public T getServlet() {
        return getArtifact();
    }

    /**
     * @since 3.0
     */
    public static class StaticMappedServletBuilder {

        private final Set<String> urlPatterns;
        private FolderResourceFactory resourceBase;
        // capturing this as a String instead of boolean to allow Jetty apply its own string to boolean parsing
        // later when our value is mixed with the servlet init params
        private String pathInfoOnly;
        private String name;

        protected StaticMappedServletBuilder(Set<String> urlPatterns) {
            this.urlPatterns = Objects.requireNonNull(urlPatterns);
        }

        public StaticMappedServletBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets an optional property that defines the "base" (or "docroot") of the static servlet. This is where the
         * files are stored. If not set, either "bq.jetty.staticResourceBase" or "bq.jetty.servlets.[name].params.resourceBase"
         * configuration properties must be defined. The latter property, if present, will override the value set here,
         * thus allowing to redefine the folder.
         *
         * @param resourceBase a path or URL of the folder where the static files are stored. Must be in a format
         *                     compatible with Bootique {@link io.bootique.resource.ResourceFactory}. E.g. this may
         *                     be a filesystem path or a "classpath:" URL.
         */
        public StaticMappedServletBuilder resourceBase(String resourceBase) {
            this.resourceBase = resourceBase != null ? new FolderResourceFactory(resourceBase) : null;
            return this;
        }

        /**
         * Optionally configures the static servlet to ignore the servlet path when resolving URLs to subdirectories.
         * Can be overridden via "bq.jetty.servlets.[name].params.pathInfoOnly" configuration property.
         */
        public StaticMappedServletBuilder pathInfoOnly() {
            this.pathInfoOnly = "true";
            return this;
        }

        public MappedServlet<?> build() {
            return new MappedServlet<>(new MultiBaseStaticServlet(resourceBase, pathInfoOnly), urlPatterns, name);
        }
    }
}
