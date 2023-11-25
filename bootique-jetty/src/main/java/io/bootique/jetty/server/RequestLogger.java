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

import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;

/**
 * Extending Jetty request logger without adding any functionality, simply to separate logging configuration between
 * Jetty and Bootique.
 *
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class RequestLogger extends CustomRequestLog {

    public RequestLogger() {
        super(getSlf4jWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT);
    }

    static Slf4jRequestLogWriter getSlf4jWriter() {
        Slf4jRequestLogWriter writer = new Slf4jRequestLogWriter();
        writer.setLoggerName(RequestLogger.class.getName());
        return writer;
    }
}
