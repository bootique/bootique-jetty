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

package io.bootique.jetty;

import javax.servlet.Filter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class MappedFilter<T extends Filter> extends MappedWebArtifact<T> {

    private int order;

    public MappedFilter(T filter, Set<String> urlPatterns, int order) {
        this(filter, urlPatterns, null, order);
    }

    /**
     * @param filter      a filter to install inside of Jetty.
     * @param urlPatterns URL patterns that this filter will respond to.
     * @param name        filter name. If null, Jetty will assign its own name.
     * @param order       an order of the filter among all the filters in a given app.
     *                    If two filters match the same request, filter with lower
     *                    ordering will be an outer filter and will be called first.
     */
    public MappedFilter(T filter, Set<String> urlPatterns, String name, int order) {
        this(filter, urlPatterns, name, Collections.emptyMap(), order);
    }

    /**
     * @param filter      a filter to install inside of Jetty.
     * @param urlPatterns URL patterns that this filter will respond to.
     * @param name        filter name. If null, Jetty will assign its own name.
     * @param params      filter init parameters map.
     * @param order       an order of the filter among all the filters in a given app.
     *                    If two filters match the same request, filter with lower
     *                    ordering will be an outer filter and will be called first.
     */
    public MappedFilter(T filter, Set<String> urlPatterns, String name, Map<String, String> params, int order) {
        super(filter, urlPatterns, name, params);
        this.order = order;
    }

    public T getFilter() {
        return getArtifact();
    }

    /**
     * Returns filter relative ordering. If two filters match the same request,
     * the filter with lower ordering will wrap the filter with higher ordering.
     *
     * @return filter relative ordering.
     */
    public int getOrder() {
        return order;
    }

}
