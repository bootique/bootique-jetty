package io.bootique.jetty.cors;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedFilter;
import io.bootique.jetty.server.ServerFactory;
import org.eclipse.jetty.servlets.CrossOriginFilter;

/**
 * @since 0.26
 */
public class BootiqueCorsModule extends ConfigModule {

    public BootiqueCorsModule() {
        super("jetty");
    }

    @Override
    public void configure(final Binder binder) {
        JettyModule.extend(binder).addMappedFilter(new TypeLiteral<MappedFilter<CrossOriginFilter>>(){});
    }

    @Singleton
    @Provides
    MappedFilter<CrossOriginFilter> provideCors(final ServerFactory factory) {
        final BootiqueCorsFactory bootiqueCors = factory.getBootiqueCors();
        if (bootiqueCors == null) {
            throw new RuntimeException("The option 'jetty:bootiqueCors' should be added to configuration yml file");
        }

        return new MappedFilter(new CrossOriginFilter(), bootiqueCors.getUrlPatterns(),
                "cors-filter", bootiqueCors.getParameters(), 1);
    }
}
