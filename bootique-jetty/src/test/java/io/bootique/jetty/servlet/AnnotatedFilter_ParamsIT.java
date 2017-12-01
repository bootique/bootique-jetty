package io.bootique.jetty.servlet;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.bootique.jetty.JettyModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AnnotatedFilter_ParamsIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testAnnotationParams() {

        testFactory.app("-s")
                .module(new FilterModule())
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/b/").request().get();
        assertEquals("p1_v1_p2_v2", r.readEntity(String.class));
    }

    @Test
    public void testConfig_Override() {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/servlet/AnnotatedFilterIT2.yml")
                .module(new FilterModule())
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        Response r = base.path("/b/").request().get();
        assertEquals("p1_v3_p2_v4", r.readEntity(String.class));
    }

    class FilterModule implements Module {

        @Override
        public void configure(Binder binder) {
            JettyModule.extend(binder).addFilter(AnnotatedFilter.class);
        }

        @Provides
        private AnnotatedFilter provideFilter() {
            return new AnnotatedFilter();
        }

        @WebFilter(filterName = "f1", urlPatterns = "/b/*", initParams = {@WebInitParam(name = "p1", value = "v1"),
                @WebInitParam(name = "p2", value = "v2")})
        class AnnotatedFilter implements Filter {

            private FilterConfig config;

            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
                this.config = filterConfig;
            }

            @Override
            public void destroy() {
                // do nothing
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                ((HttpServletResponse) response).getWriter()
                        .append("p1_" + config.getInitParameter("p1") + "_p2_" + config.getInitParameter("p2"));
            }
        }
    }

}
