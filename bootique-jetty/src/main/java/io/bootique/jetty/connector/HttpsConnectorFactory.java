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

package io.bootique.jetty.connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.resource.ResourceFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.URLResource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.URL;
import java.util.Objects;

/**
 * @since 0.18
 */
@BQConfig
@JsonTypeName("https")
public class HttpsConnectorFactory extends ConnectorFactory {

    private ResourceFactory keyStore;
    private String keyStorePassword;
    private String certificateAlias;

    public HttpsConnectorFactory() {
        keyStorePassword = "changeit";
    }

    @BQConfigProperty
    public void setKeyStore(ResourceFactory keyStore) {
        this.keyStore = keyStore;
    }

    @BQConfigProperty
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    @BQConfigProperty
    public void setCertificateAlias(String certificateAlias) {
        this.certificateAlias = certificateAlias;
    }

    @Override
    protected ConnectionFactory[] buildHttpConnectionFactories(HttpConfiguration httpConfig) {
        return new ConnectionFactory[]{
                buildSslConnectorFactory(httpConfig), buildHttp1BackingConnectorFactory(httpConfig)
        };
    }

    protected SslConnectionFactory buildSslConnectorFactory(HttpConfiguration httpConfig) {

        Objects.requireNonNull(keyStore, "'keyStore' must be specified");

        SslContextFactory contextFactory = new SslContextFactory.Server();
        URL keystoreUrl = keyStore.getUrl();
        contextFactory.setKeyStoreResource(new URLResource(keystoreUrl, null) {
        });
        contextFactory.setKeyStorePassword(keyStorePassword);
        contextFactory.setCertAlias(certificateAlias);

        return new SslConnectionFactory(contextFactory, HttpVersion.HTTP_1_1.asString());
    }

    protected HttpConnectionFactory buildHttp1BackingConnectorFactory(HttpConfiguration httpConfig) {
        return new HttpConnectionFactory(httpConfig);
    }

    @Override
    protected HttpConfiguration buildHttpConfiguration() {
        HttpConfiguration config = super.buildHttpConfiguration();
        config.addCustomizer(new SecureRequestCustomizer());
        return config;
    }
}
