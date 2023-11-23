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

package io.bootique.jetty;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.meta.config.ConfigListMetadata;
import io.bootique.meta.config.ConfigMapMetadata;
import io.bootique.meta.config.ConfigMetadataNode;
import io.bootique.meta.config.ConfigMetadataVisitor;
import io.bootique.meta.config.ConfigObjectMetadata;
import io.bootique.meta.config.ConfigValueMetadata;
import io.bootique.meta.module.ModuleMetadata;
import io.bootique.meta.module.ModulesMetadata;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JettyModuleProvider_MetadataIT {

    public static BQRuntime app = Bootique.app().autoLoadModules().createRuntime();

    @Test
    public void metadata() {

        ModulesMetadata modulesMetadata = app.getInstance(ModulesMetadata.class);
        Optional<ModuleMetadata> jettyOpt = modulesMetadata.getModules()
                .stream()
                .filter(m -> "JettyModule".equals(m.getName()))
                .findFirst();

        assertTrue(jettyOpt.isPresent());
        ModuleMetadata jetty = jettyOpt.get();

        assertTrue(jetty.getDescription().startsWith("Integrates Jetty web server"));

        assertEquals(1, jetty.getConfigs().size());
        ConfigMetadataNode rootConfig = jetty.getConfigs().stream().findFirst().get();

        assertEquals("jetty", rootConfig.getName());

        String result = rootConfig.accept(new ConfigMetadataVisitor<String>() {

            @Override
            public String visitObjectMetadata(ConfigObjectMetadata metadata) {

                StringBuilder out = new StringBuilder(metadata.getName());

                metadata.getProperties()
                        .stream()
                        .sorted(Comparator.comparing(ConfigMetadataNode::getName))
                        .forEach(p -> out.append("[").append(p.accept(this)).append("]"));

                return out.toString();
            }

            @Override
            public String visitValueMetadata(ConfigValueMetadata metadata) {
                return metadata.getName() + ":" + metadata.getType().getTypeName();
            }

            @Override
            public String visitListMetadata(ConfigListMetadata metadata) {
                return "list:" + metadata.getName() +  "<" + metadata.getElementType().getType().getTypeName() + ">";
            }

            @Override
            public String visitMapMetadata(ConfigMapMetadata metadata) {
                return "map:" + metadata.getName() + "<" + metadata.getKeysType().getTypeName() + "," +
                        metadata.getValuesType().getType().getTypeName() + ">";
            }
        });

        assertEquals("jetty" +
                "[compactPath:boolean]" +
                "[compression:boolean]" +
                "[list:connectors<io.bootique.jetty.connector.ConnectorFactory>]" +
                "[context:java.lang.String]" +
                "[map:errorPages<java.lang.Integer,java.lang.String>]" +
                "[map:filters<java.lang.String,io.bootique.jetty.server.FilterFactory>]" +
                "[idleThreadTimeout:int]" +
                "[maxFormContentSize:int]" +
                "[maxFormKeys:int]" +
                "[maxQueuedRequests:int]" +
                "[maxThreads:int]" +
                "[minThreads:int]" +
                "[map:params<java.lang.String,java.lang.String>]" +
                "[map:servlets<java.lang.String,io.bootique.jetty.server.ServletFactory>]" +
                "[sessions:boolean]" +
                "[staticResourceBase:io.bootique.resource.FolderResourceFactory]", result);
    }
}
