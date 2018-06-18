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

package io.bootique.jetty;

import io.bootique.ConfigModule;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class JettyModuleTest {

	@Test
	public void testDefaultConstructor() {
		JettyModule m = new JettyModule();
		assertEquals("jetty", getConfigPrefix(m));
	}

	@Test
	public void testPrefixConstructor() {
		JettyModule m = new JettyModule("xyz");
		assertEquals("xyz", getConfigPrefix(m));
	}

	private static String getConfigPrefix(JettyModule module) {

		try {
			Field f = ConfigModule.class.getDeclaredField("configPrefix");
			f.setAccessible(true);
			return (String) f.get(module);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
