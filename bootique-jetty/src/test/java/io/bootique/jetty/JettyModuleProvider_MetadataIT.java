package io.bootique.jetty;

import io.bootique.meta.module.ModuleMetadata;
import io.bootique.meta.module.ModulesMetadata;
import io.bootique.test.BQTestRuntime;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

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
    }
}
