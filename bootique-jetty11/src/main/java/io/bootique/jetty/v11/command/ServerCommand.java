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

package io.bootique.jetty.v11.command;

import javax.inject.Inject;
import javax.inject.Provider;

import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.meta.application.CommandMetadata;
import org.eclipse.jetty.server.Server;

public class ServerCommand extends CommandWithMetadata {

    private Provider<Server> serverProvider;

    @Inject
    public ServerCommand(Provider<Server> serverProvider) {
        super(createMetadata());
        this.serverProvider = serverProvider;
    }

    private static CommandMetadata createMetadata() {
        return CommandMetadata.builder(ServerCommand.class).description("Starts Jetty server.").build();
    }

    @Override
    public CommandOutcome run(Cli cli) {

        Server server = serverProvider.get();
        try {
            // this blocks until a successful start or an error, then releases current thread, while Jetty
            // stays running on the background
            server.start();
        } catch (Exception e) {
            return CommandOutcome.failed(1, e);
        }

        return CommandOutcome.succeededAndForkedToBackground();
    }

}
