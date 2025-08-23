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

import org.eclipse.jetty.server.handler.ContextHandler.AliasCheck;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

// TODO: This class was deprecated and removed from Jetty, but we still don't have an alternative solution (see
//  https://github.com/bootique/bootique-jetty/issues/114). So until then keeping it in Bootique.
class AllowSymLinkAliasChecker implements AliasCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllowSymLinkAliasChecker.class);

    @Override
    public boolean check(String pathInContext, Resource resource) {
        if (!(resource instanceof PathResource)) {
            return false;
        }

        PathResource pathResource = (PathResource) resource;

        try {
            Path path = pathResource.getPath();
            Path alias = pathResource.getAliasPath();

            if (PathResource.isSameName(alias, path)) {
                return false;
            }

            if (hasSymbolicLink(path) && Files.isSameFile(path, alias)) {
                LOGGER.debug("Allow symlink {} --> {}", resource, pathResource.getAliasPath());
                return true;
            }
        } catch (Exception e) {
            LOGGER.trace("IGNORED", e);
        }

        return false;
    }

    private boolean hasSymbolicLink(Path path) {
        if (Files.isSymbolicLink(path)) {
            return true;
        }

        Path base = path.getRoot();
        for (Path segment : path) {
            base = base.resolve(segment);
            if (Files.isSymbolicLink(base)) {
                return true;
            }
        }

        return false;
    }
}
