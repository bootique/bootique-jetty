package io.bootique.jetty;

import com.google.inject.Module;
import io.bootique.BQModule;
import io.bootique.BQModuleProvider;
import io.bootique.jetty.server.ServerFactory;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

public class JettyModuleProvider implements BQModuleProvider {

	@Override
	public Module module() {
		return new JettyModule();
	}

	/**
	 * @since 0.19
	 * @return a single entry map with {@link ServerFactory}.
	 */
	@Override
	public Map<String, Type> configs() {
		// TODO: config prefix is hardcoded. Refactor away from ConfigModule, and make provider
		// generate config prefix, reusing it in metadata...
		return Collections.singletonMap("jetty", ServerFactory.class);
	}

	@Override
	public BQModule.Builder moduleBuilder() {
		return BQModuleProvider.super
				.moduleBuilder()
				.description("Integrates Jetty web server in the application.");
	}
}
