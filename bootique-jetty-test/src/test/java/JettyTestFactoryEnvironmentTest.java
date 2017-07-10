import io.bootique.BQRuntime;
import io.bootique.jetty.server.ServerFactory;
import io.bootique.jetty.test.junit.JettyTestFactory;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JettyTestFactoryEnvironmentTest {

    @Rule
    public JettyTestFactory testFactory = new JettyTestFactory();

    @Test
    public void testYAMLContext() {
        BQRuntime runtime = testFactory.app("--config=src/test/resources/envtest.yml").autoLoadModules().start();
        ServerFactory serverFactory = runtime.getInstance(ServerFactory.class);

        //BQ release needed
        //assertEquals(serverFactory.getContext(), "/myapp");
    }
}
