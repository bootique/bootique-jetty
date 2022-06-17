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
package io.bootique.jetty.jakarta.cors;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.jetty.MappedFilter;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@BQConfig
public class CrossOriginFilterFactory {

    private Set<String> urlPatterns;

    private String allowedOrigins;
    private String allowedTimingOrigins;
    private String allowedMethods;
    private String allowedHeaders;
    private int preflightMaxAge;
    private boolean allowCredentials;
    private String exposedHeaders;
    private boolean chainPreflight;
    private int order;

    public CrossOriginFilterFactory() {
        this.allowedOrigins = "*";
        this.allowedTimingOrigins = "*";
        this.allowedMethods = "GET,POST,HEAD";
        this.allowedHeaders = "X-Requested-With,Content-Type,Accept,Origin";
        this.preflightMaxAge = 1800;
        this.allowCredentials = true;
        this.exposedHeaders = "";
        this.chainPreflight = true;
        this.order = 1;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    @BQConfigProperty
    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getAllowedTimingOrigins() {
        return allowedTimingOrigins;
    }

    @BQConfigProperty
    public void setAllowedTimingOrigins(String allowedTimingOrigins) {
        this.allowedTimingOrigins = allowedTimingOrigins;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    @BQConfigProperty
    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    @BQConfigProperty
    public void setAllowedHeaders(String allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public int getPreflightMaxAge() {
        return preflightMaxAge;
    }

    @BQConfigProperty
    public void setPreflightMaxAge(int preflightMaxAge) {
        this.preflightMaxAge = preflightMaxAge;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    @BQConfigProperty
    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public String getExposedHeaders() {
        return exposedHeaders;
    }

    @BQConfigProperty
    public void setExposedHeaders(String exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public boolean isChainPreflight() {
        return chainPreflight;
    }

    @BQConfigProperty
    public void setChainPreflight(boolean chainPreflight) {
        this.chainPreflight = chainPreflight;
    }

    private Set<String> getUrlPatterns() {
        return urlPatterns != null ? urlPatterns : Collections.singleton("/*");
    }

    @BQConfigProperty
    public void setUrlPatterns(Set<String> urlPatterns) {
        this.urlPatterns = urlPatterns;
    }


    public int getOrder() {
        return order;
    }

    @BQConfigProperty
    public void setOrder(int order) {
        this.order = order;
    }

    private Map<String, String> getParameters() {
        final Map<String, String> params = new HashMap<>();
        params.put(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, getAllowedOrigins());
        params.put(CrossOriginFilter.ALLOWED_TIMING_ORIGINS_PARAM, getAllowedTimingOrigins());
        params.put(CrossOriginFilter.ALLOWED_METHODS_PARAM, getAllowedMethods());
        params.put(CrossOriginFilter.ALLOWED_HEADERS_PARAM, getAllowedHeaders());
        params.put(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM, String.valueOf(getPreflightMaxAge()));
        params.put(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, String.valueOf(isAllowCredentials()));
        params.put(CrossOriginFilter.EXPOSED_HEADERS_PARAM, getExposedHeaders());
        params.put(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, String.valueOf(isChainPreflight()));

        return params;
    }

    public MappedFilter<CrossOriginFilter> createCorsFilter() {
        return new MappedFilter<>(
                new CrossOriginFilter(),
                getUrlPatterns(),
                "cors-filter",
                getParameters(),
                getOrder());
    }
}
