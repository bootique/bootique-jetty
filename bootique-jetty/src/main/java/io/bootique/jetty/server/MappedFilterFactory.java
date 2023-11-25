/**
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * “License”); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jetty.server;

import io.bootique.jetty.MappedFilter;

import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * A factory that analyzes Filter annotations to create a {@link MappedFilter} out of a {@link Filter}.
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class MappedFilterFactory {

    public MappedFilter toMappedFilter(Filter filter, int order) {

        WebFilter wfAnnotation = filter.getClass().getAnnotation(WebFilter.class);

        if (wfAnnotation == null) {
            throw new IllegalArgumentException(
                    "Filter contains no @WebFilter annotation and can not be mapped directly. Wrap it in a MappedFilter instead.");
        }

        String name = wfAnnotation.filterName() != null && wfAnnotation.filterName().length() > 0
                ? wfAnnotation.filterName() : null;
        Set<String> urlPatterns = new HashSet<>(asList(wfAnnotation.urlPatterns()));

        Map<String, String> initParams = new HashMap<>();

        WebInitParam[] paramsArray = wfAnnotation.initParams();
        if (paramsArray != null) {
            asList(paramsArray).forEach(p -> initParams.put(p.name(), p.value()));
        }

        return new MappedFilter(filter, urlPatterns, name, initParams, order);
    }

}
