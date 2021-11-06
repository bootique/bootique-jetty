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
package io.bootique.jetty.servlet;

import org.eclipse.jetty.servlet.DefaultServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @since 2.0
 */
public class StaticServlet extends DefaultServlet {

    static final String RESOURCE_BASE_PARAMETER = "resourceBase";

    private String resourceBase;

    public StaticServlet(String resourceBase) {
        this.resourceBase = resourceBase;
    }

    @Override
    public String getInitParameter(String name) {
        // ignore super value if the parameter is "resourceBase"
        return RESOURCE_BASE_PARAMETER.equals(name) ? this.resourceBase : super.getInitParameter(name);
    }

    // making public, so we can call it from MultiBaseDefaultServlet
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doGet(request, response);
    }
}
