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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ConditionalHandler;
import org.eclipse.jetty.server.handler.CrossOriginHandler;

import java.time.Duration;
import java.util.Set;

/**
 * Configures Jetty's {@link CrossOriginHandler} for CORS support. CORS is only installed in the request handling chain
 * when at least one "urlPatterns" entry is configured, so it adds no overhead to applications that don't use it.
 *
 * @since 4.0
 */
@BQConfig("""
        Configures CORS support via Jetty's CrossOriginHandler. Only installed when at least one "urlPatterns" entry is configured.""")
public class CrossOriginHandlerFactory {

    private Set<String> allowedOrigins;
    private Set<String> allowedTimingOrigins;
    private Set<String> allowedMethods;
    private Set<String> allowedHeaders;
    private Set<String> exposedHeaders;
    private int preflightMaxAge;
    private boolean allowCredentials;
    private boolean chainPreflight;
    private Set<String> urlPatterns;

    public CrossOriginHandlerFactory() {
        this.allowedOrigins = Set.of("*");
        this.allowedTimingOrigins = Set.of("*");
        this.allowedMethods = Set.of("GET", "POST", "HEAD");
        this.allowedHeaders = Set.of("X-Requested-With", "Content-Type", "Accept", "Origin");
        this.preflightMaxAge = 1800;
        this.allowCredentials = false;
        this.chainPreflight = true;
    }

    @BQConfigProperty("""
            Origins allowed to make cross-origin requests, as a set of origin patterns (e.g. "https://example.com"). \
            Default is "*" (any origin). Note that "*" combined with allowCredentials set to 'true' is insecure and \
            should be avoided.""")
    public void setAllowedOrigins(Set<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @BQConfigProperty("Origins allowed to receive resource timing information via the Timing-Allow-Origin header.")
    public void setAllowedTimingOrigins(Set<String> allowedTimingOrigins) {
        this.allowedTimingOrigins = allowedTimingOrigins;
    }

    @BQConfigProperty("HTTP methods allowed in cross-origin requests. Default is 'GET', 'POST', 'HEAD'.")
    public void setAllowedMethods(Set<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    @BQConfigProperty("""
            Request headers allowed in cross-origin requests. Default is 'X-Requested-With', 'Content-Type', 'Accept', \
            'Origin'.""")
    public void setAllowedHeaders(Set<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    @BQConfigProperty("Response headers exposed to the client. Empty by default.")
    public void setExposedHeaders(Set<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    @BQConfigProperty("Number of seconds a preflight response may be cached by the client. Default is 1800.")
    public void setPreflightMaxAge(int preflightMaxAge) {
        this.preflightMaxAge = preflightMaxAge;
    }

    @BQConfigProperty("""
            Whether the client may send credentials (cookies, authorization headers) with cross-origin requests. \
            Default is 'false'. Note that 'true' combined with the default allowedOrigins of '*' is insecure; when \
            enabling credentials you should also narrow allowedOrigins to specific origins.""")
    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    @BQConfigProperty("""
            Whether preflight (OPTIONS) requests are chained to the target resource for normal handling after the CORS \
            headers are added. When 'false', the CORS handler fully handles the preflight request and does not pass it \
            down. Default is 'true'.""")
    public void setChainPreflight(boolean chainPreflight) {
        this.chainPreflight = chainPreflight;
    }

    @BQConfigProperty("""
            URL patterns that CORS applies to. At least one pattern must be set for CORS to be activated; to apply \
            CORS to the entire context use "/*". CORS is only applied to requests matching one of these patterns, and \
            bypassed for all others.""")
    public void setUrlPatterns(Set<String> urlPatterns) {
        this.urlPatterns = urlPatterns;
    }

    /**
     * Creates a handler wrapping the provided downstream handler. CORS is only activated when at least one
     * "urlPatterns" entry is configured; otherwise the downstream handler is returned unchanged. When activated, a
     * {@link CrossOriginHandler} is wrapped in a {@link ConditionalHandler.SkipNext} so that CORS is only applied to
     * matching requests, and bypassed for all others.
     */
    public Handler createHandler(Handler downstream) {

        // CORS is only activated when at least one URL pattern is configured. Without explicit patterns there is
        // nothing for CORS to apply to, so the handler is left out of the chain entirely.
        if (urlPatterns == null || urlPatterns.isEmpty()) {
            return downstream;
        }

        CrossOriginHandler corsHandler = new CrossOriginHandler();

        if (allowedOrigins != null) {
            corsHandler.setAllowedOriginPatterns(allowedOrigins);
        }
        if (allowedTimingOrigins != null) {
            corsHandler.setAllowedTimingOriginPatterns(allowedTimingOrigins);
        }
        if (allowedMethods != null) {
            corsHandler.setAllowedMethods(allowedMethods);
        }
        if (allowedHeaders != null) {
            corsHandler.setAllowedHeaders(allowedHeaders);
        }
        if (exposedHeaders != null) {
            corsHandler.setExposedHeaders(exposedHeaders);
        }
        corsHandler.setPreflightMaxAge(Duration.ofSeconds(preflightMaxAge));
        corsHandler.setAllowCredentials(allowCredentials);
        corsHandler.setDeliverPreflightRequests(chainPreflight);

        corsHandler.setHandler(downstream);

        // Scope CORS to the configured paths: a SkipNext wrapper bypasses the CORS handler (going straight to the
        // downstream handler) whenever its conditions are met. By excluding the CORS paths from those conditions, the
        // conditions are NOT met for them, so CORS runs; for all other paths the conditions are met and CORS is skipped.
        ConditionalHandler.SkipNext skipNext = new ConditionalHandler.SkipNext();
        skipNext.setHandler(corsHandler);
        urlPatterns.forEach(skipNext::excludePath);

        return skipNext;
    }
}
