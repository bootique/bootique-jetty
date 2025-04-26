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

    private final FolderResourceFactory resourceBase;
    // capturing this as a String instead of boolean to allow Jetty apply its own string to boolean parsing
    private final String pathInfoOnly;

    private DoGetProcessor doGetProcessor;
    private List<StaticServlet> delegates;

    /**
     * @since 3.0
     */
    public MultiBaseStaticServlet(FolderResourceFactory resourceBase, String pathInfoOnly) {
        this.resourceBase = resourceBase;
        this.pathInfoOnly = pathInfoOnly;
    }

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

        String pathInfoOnly = resolvePathInfoOnly();
        Collection<URL> resourceBases = resolveResourceBases();

        // "classpath:" URLs can point to multiple locations. Map them to multiple delegated servlets
        List<StaticServlet> delegates = new ArrayList<>(resourceBases.size());
        for (URL baseUrl : resourceBases) {
            delegates.add(new StaticServlet(baseUrl.toExternalForm(), pathInfoOnly));
        }

        if (delegates.isEmpty()) {
            return Collections.singletonList(new StaticServlet(null, pathInfoOnly));
        } else if (delegates.size() > 1) {
            LOGGER.info("Found multiple base URLs for resource base '{}': {}", resourceBase, resourceBases);
        }

        return delegates;
    }

    protected Collection<URL> resolveResourceBases() {
        FolderResourceFactory resourceBase = resolveResourceBase();
        try {
            return resourceBase != null ? resourceBase.getUrls() : Collections.emptyList();
        } catch (IllegalArgumentException e) {

            // log, but allow to start
            // TODO: why are we so lenient here, should we throw?

            LOGGER.warn("Static servlet resource base folder '{}' does not exist", resourceBase.getResourceId());
            return Collections.emptyList();
        }
    }

    protected FolderResourceFactory resolveResourceBase() {
        // this.resourceBase is allowed to be null; also it can be overridden by the servlet parameter
        String paramResourceBase = getInitParameter(StaticServlet.RESOURCE_BASE_PARAMETER);
        return paramResourceBase != null ? new FolderResourceFactory(paramResourceBase) : this.resourceBase;
    }

    protected String resolvePathInfoOnly() {
        String paramValue = getInitParameter(StaticServlet.PATH_INFO_ONLY_PARAMETER);
        return paramValue != null ? paramValue : this.pathInfoOnly;
    }

    @FunctionalInterface
    interface DoGetProcessor {
        void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
    }

    static class DoGetOne implements DoGetProcessor {

        private final StaticServlet delegate;

        DoGetOne(StaticServlet delegate) {
            this.delegate = delegate;
        }

        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            delegate.doGet(req, resp);
        }
    }

    static class DoGetMany implements DoGetProcessor {

        private final StaticServlet[] delegates;

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
