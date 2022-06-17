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
package io.bootique.jetty.junit5;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.bootique.config.jackson.JsonConfigurationLoader;
import io.bootique.config.jackson.PropertiesConfigurationLoader;
import io.bootique.config.jackson.merger.InPlacePropertiesMerger;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.0.M1
 */
public class JettyTesterConfigLoader implements JsonConfigurationLoader {

    public static final int ORDER = PropertiesConfigurationLoader.ORDER - 5;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public JsonNode updateConfiguration(JsonNode mutableInput) {

        // remove all connector configs
        JsonNode jetty = mutableInput.get("jetty");
        if (jetty != null && jetty instanceof ObjectNode) {
            ((ObjectNode) jetty).remove("connectors");
        }

        // create a single test connector
        Map<String, String> properties = new HashMap<>();
        properties.put("jetty.connectors[0].port", "any");
        return new InPlacePropertiesMerger(properties).apply(mutableInput);
    }
}
