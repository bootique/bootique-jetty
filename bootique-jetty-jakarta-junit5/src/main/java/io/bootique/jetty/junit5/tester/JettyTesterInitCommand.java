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
package io.bootique.jetty.junit5.tester;

import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.jetty.server.ServerHolder;
import io.bootique.meta.application.CommandMetadata;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @since 3.0.M1
 */
public class JettyTesterInitCommand extends CommandWithMetadata {

    private final Provider<ServerHolder> serverHolder;
    private final JettyTesterBootiqueHook testerHook;

    @Inject
    public JettyTesterInitCommand(JettyTesterBootiqueHook testerHook, Provider<ServerHolder> serverHolder) {
        super(buildMetadata());
        this.serverHolder = serverHolder;
        this.testerHook = testerHook;
    }

    private static CommandMetadata buildMetadata() {
        return CommandMetadata.builder(JettyTesterInitCommand.class)
                .description("Connects JettyTester to a given app runtime")
                .hidden()
                .build();
    }

    @Override
    public CommandOutcome run(Cli cli) {

        ServerHolder holderResolved = serverHolder.get();
        testerHook.init(holderResolved.getContext(), JettyConnectorAccessor.getConnectorHolder(holderResolved));

        return CommandOutcome.succeeded();
    }
}
