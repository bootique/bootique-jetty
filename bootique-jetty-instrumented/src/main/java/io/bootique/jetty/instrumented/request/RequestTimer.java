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

package io.bootique.jetty.instrumented.request;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import java.util.Objects;

/**
 * Provides timing metrics for request execution, optionally logging of request start and finish marks.
 *
 * @since 0.15
 */
public class RequestTimer implements ServletRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTimer.class);
    private static final String TIMER_KEY = RequestTimer.class.getName();

    private Timer requestTimer;

    public RequestTimer(Timer requestTimer) {
        this.requestTimer = requestTimer;
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        Timer.Context requestTimerContext = requestTimer.time();
        sre.getServletRequest().setAttribute(TIMER_KEY, requestTimerContext);
        LOGGER.info("started");
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {

        // note that we are skipping request parameter/URL/etc. logging...
        // This is done by Slf4jRequestLog. Here we only log timing

        Timer.Context requestTimerContext = (Timer.Context) sre.getServletRequest().getAttribute(TIMER_KEY);
        Objects.requireNonNull(requestTimerContext, "No timer found at the end of request");
        long timeNanos = requestTimerContext.stop();
        LOGGER.info("finished in {} ms", timeNanos / 1000000);
    }
}
