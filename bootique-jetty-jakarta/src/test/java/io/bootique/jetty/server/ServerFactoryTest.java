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
package io.bootique.jetty.server;

import io.bootique.jetty.request.RequestMDCManager;
import io.bootique.shutdown.ShutdownManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class ServerFactoryTest {

    private ServerFactory createWithDefaults() {
        return new ServerFactory(
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                mock(RequestMDCManager.class),
                mock(ShutdownManager.class)
        );
    }

    @Test
    public void resolveContext_Default() {
        ServerFactory factory = createWithDefaults();
        assertEquals("/", factory.resolveContext());
    }

    @Test
    public void resolveContext_Root() {
        ServerFactory factory = createWithDefaults();
        factory.setContext("/");
        assertEquals("/", factory.resolveContext());
    }

    @Test
    public void resolveContext_NonRoot() {
        ServerFactory factory = createWithDefaults();
        factory.setContext("/myapp");
        assertEquals("/myapp", factory.resolveContext());
    }

    @Test
    public void resolveContext_MissingLeadingSlash() {
        ServerFactory factory = createWithDefaults();
        factory.setContext("myapp");
        assertEquals("/myapp", factory.resolveContext());
    }

    @Test
    public void resolveContext_ExtraTrailinglash() {
        ServerFactory factory = createWithDefaults();
        factory.setContext("/myapp/");
        assertEquals("/myapp", factory.resolveContext());
    }
}
