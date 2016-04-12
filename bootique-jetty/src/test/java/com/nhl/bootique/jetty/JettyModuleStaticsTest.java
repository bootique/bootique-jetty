package com.nhl.bootique.jetty;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

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
