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
package io.bootique.jetty.servlet;

import io.bootique.resource.FolderResourceFactory;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @since 2.0
 */
public class MultiBaseStaticServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiBaseStaticServlet.class);

    private DoGetProcessor doGetProcessor;
    private List<StaticServlet> delegates;

    // overriding methods overridden in the Jetty DefaultServlet to proxy them properly

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.delegates = createDelegates();
        this.doGetProcessor = delegates.size() == 1
                ? new DoGetOne(delegates.get(0))
                : new DoGetMany(delegates.toArray(new StaticServlet[0]));

        for (HttpServlet ds : delegates) {
            ds.init(config);
        }
    }

    @Override
    public void destroy() {
        for (HttpServlet ds : delegates) {
            ds.destroy();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGetProcessor.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Allow", "GET,HEAD,POST,OPTIONS");
    }

    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected List<StaticServlet> createDelegates() {
        String resourceBase = getInitParameter(StaticServlet.RESOURCE_BASE_PARAMETER);
        if (resourceBase == null) {
            return Collections.singletonList(new StaticServlet(null));
        }

        Collection<URL> resourceBaseUrls = resolveFolderResourceFactory(resourceBase);
        if (resourceBaseUrls.isEmpty()) {
            return Collections.singletonList(new StaticServlet(null));
        }

        // "classpath:" URLs can point to multiple locations. Map them to multiple delegated servlets
        List<StaticServlet> delegates = new ArrayList<>(resourceBaseUrls.size());
        for (URL baseUrl : resourceBaseUrls) {
            delegates.add(new StaticServlet(baseUrl.toExternalForm()));
        }

        if(delegates.size() > 1) {
            LOGGER.info("Found multiple base URLs for resource base '{}': {}", resourceBase, resourceBaseUrls);
        }

        return delegates;
    }

    protected Collection<URL> resolveFolderResourceFactory(String path) {
        try {
            return new FolderResourceFactory(path).getUrls();
        } catch (IllegalArgumentException e) {
            // log, but allow to start
            LOGGER.warn("Static servlet base directory '{}' does not exist", path);
            return Collections.emptyList();
        }
    }

    @FunctionalInterface
    interface DoGetProcessor {
        void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
    }

    static class DoGetOne implements DoGetProcessor {

        private StaticServlet delegate;

        DoGetOne(StaticServlet delegate) {
            this.delegate = delegate;
        }

        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            delegate.doGet(req, resp);
        }
    }

    static class DoGetMany implements DoGetProcessor {

        private StaticServlet[] delegates;

        public DoGetMany(StaticServlet[] delegates) {
            this.delegates = delegates;
        }

        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            for (int i = 0; i < delegates.length; i++) {

                if (i > 0) {
                    resp.reset();
                }

                delegates[i].doGet(req, resp);

                // first successfully found resource wins ...
                if (resp.getStatus() != HttpServletResponse.SC_NOT_FOUND) {
                    return;
                }
            }

        }
    }
}
