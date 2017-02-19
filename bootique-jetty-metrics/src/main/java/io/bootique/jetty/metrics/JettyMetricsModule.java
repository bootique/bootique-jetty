package io.bootique.jetty.metrics;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedServlet;
import io.bootique.metrics.health.HealthCheckRegistry;

import java.util.Collections;

/**
 * @since 0.20
 */
public class JettyMetricsModule implements Module {

    @Override
    public void configure(Binder binder) {
        JettyModule.extend(binder).addMappedServlet(new TypeLiteral<MappedServlet<HealthCheckServlet>>() {
        });
    }

    @Singleton
    @Provides
    MappedServlet<HealthCheckServlet> provideHealthCheckServlet(HealthCheckRegistry registry) {
        HealthCheckServlet servlet = new HealthCheckServlet(registry);
        return new MappedServlet<HealthCheckServlet>(servlet, Collections.singleton("/health"), "health");
    }
}
