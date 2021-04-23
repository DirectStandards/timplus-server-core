package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;


import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultDomainProvider_getDomainNamesTest extends SpringDataBaseTest
{
	protected final DefaultDomainProvider prov = new DefaultDomainProvider();
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		prov.createDomain("domain1.com", true);
		prov.createDomain("domain2.com", false);
	}
	
	@Test
	public void testGetAllDomainNames_assertDomainNames() throws Exception
	{
		Collection<String> domains = prov.getDomainNames(false);
		assertEquals(2, domains.size());
		
		assertThat(domains, containsInAnyOrder("domain1.com", "domain2.com"));
	}
	
	@Test
	public void testGetEnabledDomainNames_assertDomainNames() throws Exception
	{
		Collection<String> domains = prov.getDomainNames(true);
		assertThat(domains, containsInAnyOrder("domain1.com"));
		
	}
}
