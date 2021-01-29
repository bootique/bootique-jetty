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
package io.bootique.jetty.websocket;

import org.slf4j.MDC;

import javax.websocket.Session;
import java.util.Map;

/**
 * Creates an MDC context for websocket operations, with the context values originating from the HTTP request that
 * opened the websocket.
 *
 * @since 2.0.B1
 */
public class WebSocketMDCManager {

    public static final String MDC_MAP_KEY = "io.bootique.jetty.websocket.mdc";

    public void run(Session session, WebSocketMDCOperation op) {

        Map<String, String> mdc = initMDC(session.getUserProperties());
        try {
            runBare(op);
        } finally {
            clearMDC(mdc);
        }
    }

    protected void runBare(WebSocketMDCOperation op) {
        try {
            op.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    protected Map<String, String> initMDC(Map<String, Object> sessionProps) {
        Map<String, String> mdc = (Map<String, String>) sessionProps.get(MDC_MAP_KEY);

        if (mdc != null) {
            mdc.forEach(MDC::put);
        }

        return mdc;
    }

    protected void clearMDC(Map<String, String> mdc) {
        if (mdc != null) {
            mdc.forEach((k, v) -> MDC.remove(k));
        }
    }
}
