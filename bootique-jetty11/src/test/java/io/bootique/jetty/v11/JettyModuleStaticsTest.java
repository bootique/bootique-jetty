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

package io.bootique.jetty.v11;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JettyModuleStaticsTest {

	@Test
	public void testMaxOrder_Empty() {
		assertEquals(0, JettyModule.maxOrder(Collections.emptySet()));
	}
	
	@Test
	public void testMaxOrder() {
		Set<MappedFilter> filters = new LinkedHashSet<>();
		filters.add(new MappedFilter(null, null, -1));
		filters.add(new MappedFilter(null, null, 35));
		filters.add(new MappedFilter(null, null, 0));
		filters.add(new MappedFilter(null, null, 12));
		
		assertEquals(35, JettyModule.maxOrder(filters));
	}

}
