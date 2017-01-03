package io.bootique.jetty;

import io.bootique.meta.config.ConfigListMetadata;
import io.bootique.meta.config.ConfigMapMetadata;
import io.bootique.meta.config.ConfigMetadataNode;
import io.bootique.meta.config.ConfigMetadataVisitor;
import io.bootique.meta.config.ConfigObjectMetadata;
import io.bootique.meta.config.ConfigValueMetadata;
import io.bootique.meta.module.ModuleMetadata;
import io.bootique.meta.module.ModulesMetadata;
import io.bootique.test.BQTestRuntime;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.Comparator;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JettyModuleProvider_MetadataIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testMetadata() {

        BQTestRuntime runtime = testFactory.app().autoLoadModules().createRuntime();

        ModulesMetadata modulesMetadata = runtime.getRuntime().getInstance(ModulesMetadata.class);
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
                return "list:" + metadata.getName();
            }

            @Override
            public String visitMapMetadata(ConfigMapMetadata metadata) {
                return "map:" + metadata.getName();
            }
        });

        assertEquals("jetty" +
                "[compression:boolean]" +
                "[list:connectors]" +
                "[context:java.lang.String]" +
                "[map:filters]" +
                "[idleThreadTimeout:int]" +
                "[maxQueuedRequests:int]" +
                "[maxThreads:int]" +
                "[minThreads:int]" +
                "[map:params]" +
                "[map:servlets]" +
                "[sessions:boolean]" +
                "[staticResourceBase:io.bootique.resource.FolderResourceFactory]", result);
    }
}
