package io.bootique.jetty.cors;

import io.bootique.annotation.BQConfigProperty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.eclipse.jetty.servlets.CrossOriginFilter.*;

/**
 * @since 0.26
 */
public class BootiqueCorsFactory {

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

    public BootiqueCorsFactory(){

        this.urlPatterns = new HashSet<>();
        this.allowedOrigins = "*";
        this.allowedTimingOrigins = "*";
        this.allowedMethods = "X-Requested-With,Content-Type,Accept,Origin";
        this.allowedHeaders = "GET,POST,HEAD";
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

    public Set<String> getUrlPatterns() {
        return urlPatterns;
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

    public Map<String, String> getParameters() {
        final Map<String, String> params = new HashMap<>();
        params.put(ALLOWED_ORIGINS_PARAM, getAllowedOrigins());
        params.put(ALLOWED_TIMING_ORIGINS_PARAM, getAllowedTimingOrigins());
        params.put(ALLOWED_METHODS_PARAM, getAllowedMethods());
        params.put(ALLOWED_HEADERS_PARAM, getAllowedHeaders());
        params.put(PREFLIGHT_MAX_AGE_PARAM, String.valueOf(getPreflightMaxAge()));
        params.put(ALLOW_CREDENTIALS_PARAM, String.valueOf(isAllowCredentials()));
        params.put(EXPOSED_HEADERS_PARAM, getExposedHeaders());
        params.put(CHAIN_PREFLIGHT_PARAM, String.valueOf(isChainPreflight()));

        return params;
    }

}
