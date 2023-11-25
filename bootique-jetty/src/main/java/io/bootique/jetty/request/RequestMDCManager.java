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

package io.bootique.jetty.request;

import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Initializes and clears a preconfigured set of MDC logging keys at the beginning and the end of each request.
 *
 * @since 2.0
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
// Not using the standard ServletListener, and going for the internal Jetty API to intercept maximally wide scope
// to provide context to
public class RequestMDCManager implements HttpChannel.Listener {

    private final Map<String, RequestMDCItem> items;

    public RequestMDCManager(Map<String, RequestMDCItem> items) {
        this.items = Objects.requireNonNull(items);
    }

    @Override
    public void onRequestBegin(Request request) {
        items.values().forEach(e -> e.initMDC(request.getContext(), request));
    }

    @Override
    public void onComplete(Request request) {
        items.values().forEach(e -> e.cleanupMDC(request.getContext(), request));
    }

    public Set<String> mdcKeys() {
        return items.keySet();
    }
}
